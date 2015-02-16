package services

import com.gu.membership.salesforce.Member.Keys
import com.gu.membership.salesforce._
import com.gu.membership.stripe.Stripe
import com.gu.membership.stripe.Stripe.Customer
import com.gu.membership.util.Timing
import com.typesafe.scalalogging.slf4j.LazyLogging
import configuration.Config
import controllers.IdentityRequest
import forms.MemberForm._
import model.Benefits.DiscountTicketTiers
import model.Eventbrite.EBCode
import model.RichEvent.RichEvent
import model.RichEvent._
import model.Zuora.PreviewInvoiceItem
import model.{IdMinimalUser, IdUser, PaidTierPlan, ProductRatePlan}
import model._
import monitoring.MemberMetrics
import play.api.libs.json.Json
import services.EventbriteService._
import utils.ScheduledTask

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

case class MemberServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}

class FrontendMemberRepository(salesforceConfig: SalesforceConfig) extends MemberRepository {
  val metrics = new MemberMetrics(salesforceConfig.envName)

  val salesforce = new Scalaforce {
    val consumerKey = salesforceConfig.consumerKey
    val consumerSecret = salesforceConfig.consumerSecret

    val apiURL = salesforceConfig.apiURL.toString
    val apiUsername = salesforceConfig.apiUsername
    val apiPassword = salesforceConfig.apiPassword
    val apiToken = salesforceConfig.apiToken

    val stage = Config.stage
    val application = "Frontend"

    val authTask = ScheduledTask("", Authentication("", ""), 0.seconds, 30.minutes)(getAuthentication)

    def authentication: Authentication = authTask.get()
  }
}

trait MemberService extends LazyLogging {
  def initialData(user: IdUser, formData: JoinForm) = {
    Seq(Json.obj(
      Keys.EMAIL -> user.primaryEmailAddress,
      Keys.FIRST_NAME -> formData.name.first,
      Keys.LAST_NAME -> formData.name.last,
      Keys.MAILING_STREET -> formData.deliveryAddress.line,
      Keys.MAILING_CITY -> formData.deliveryAddress.town,
      Keys.MAILING_STATE -> formData.deliveryAddress.countyOrState,
      Keys.MAILING_POSTCODE -> formData.deliveryAddress.postCode,
      Keys.MAILING_COUNTRY -> formData.deliveryAddress.country.alpha2,
      Keys.ALLOW_MEMBERSHIP_MAIL -> true
    )) ++ Map(
      Keys.ALLOW_THIRD_PARTY_EMAIL -> formData.marketingChoices.thirdParty,
      Keys.ALLOW_GU_RELATED_MAIL -> formData.marketingChoices.gnm
    ).collect { case (k, Some(v)) => Json.obj(k -> v) }
  }.reduce(_ ++ _)

  def memberData(plan: ProductRatePlan, customerOpt: Option[Stripe.Customer]) = Json.obj(
    Keys.TIER -> plan.salesforceTier
  ) ++ customerOpt.map { customer =>
    Json.obj(
      Keys.CUSTOMER_ID -> customer.id,
      Keys.DEFAULT_CARD_ID -> customer.card.id
    )
  }.getOrElse(Json.obj())

  def createMember(user: IdMinimalUser, formData: JoinForm, identityRequest: IdentityRequest): Future[MemberId] = {
    val touchpointBackend = TouchpointBackend.forUser(user)
    val identityService = IdentityService(IdentityApi)

    Timing.record(touchpointBackend.memberRepository.metrics, "createMember") {
      def futureCustomerOpt = formData match {
        case paid: PaidMemberJoinForm => touchpointBackend.stripeService.Customer.create(user.id, paid.payment.token).map(Some(_))
        case _ => Future.successful(None)
      }

      formData.password.map(identityService.updateUserPassword(_, identityRequest, user.id))

      for {
        fullUser <- identityService.getFullUserDetails(user, identityRequest)
        customerOpt <- futureCustomerOpt
        memberId <- touchpointBackend.memberRepository.upsert(user.id, initialData(fullUser, formData))
        subscription <- touchpointBackend.subscriptionService.createSubscription(memberId, formData, customerOpt)

        // Set some fields once subscription has been successful
        updatedMember <- touchpointBackend.memberRepository.upsert(user.id, memberData(formData.plan, customerOpt))
      } yield {
        identityService.updateUserFieldsBasedOnJoining(user, formData, identityRequest)

        touchpointBackend.memberRepository.metrics.putSignUp(formData.plan)
        memberId
      }
    }.andThen {
      case Success(memberAccount) => logger.debug(s"createMember() success user=${user.id} memberAccount=$memberAccount")
      case Failure(error) => {
        logger.error(s"Error in createMember() user=${user.id}", error)
        touchpointBackend.memberRepository.metrics.putFailSignUp(formData.plan)
      }
    }
  }

  def createDiscountForMember(member: Member, event: RichEvent): Future[Option[EBCode]] = (for {
      ticketing <- event.internalTicketing
      benefit <- ticketing.memberDiscountOpt if DiscountTicketTiers.contains(member.tier)
    } yield {
      // Add a "salt" to make access codes different to discount codes
      val code = DiscountCode.generate(s"A_${member.identityId}_${event.id}")
      event.service.createOrGetAccessCode(event, code, ticketing.memberBenefitTickets).map(Some(_))
    }).getOrElse(Future.successful(None))

  def upgradeFreeSubscription(freeMember: FreeMember, user: IdMinimalUser, newTier: Tier, form: FreeMemberChangeForm,
                              identityRequest: IdentityRequest): Future[MemberId] = {
    val touchpointBackend = TouchpointBackend.forUser(user)

    for {
      customer <- touchpointBackend.stripeService.Customer.create(user.id, form.payment.token)
      paymentResult <- touchpointBackend.subscriptionService.createPaymentMethod(freeMember, customer)
      memberId <- upgradeSubscription(freeMember, user, newTier, Some(form), form.payment.annual, Some(customer), identityRequest)
    } yield memberId
  }

  def upgradePaidSubscription(paidMember: PaidMember, user: IdMinimalUser, newTier: Tier,
                              identityRequest: IdentityRequest): Future[MemberId] = {
    for {
      paymentSummary <- TouchpointBackend.forUser(user).subscriptionService.getPaymentSummary(paidMember)
      memberId <- upgradeSubscription(paidMember, user, newTier, None, paymentSummary.current.annual, None, identityRequest)
    } yield memberId

  }

  private def upgradeSubscription(member: Member, user: IdMinimalUser, newTier: Tier, form: Option[MemberChangeForm],
                                  annual: Boolean, customerOpt: Option[Customer], identityRequest: IdentityRequest): Future[MemberId] = {
    val touchpointBackend = TouchpointBackend.forUser(user)
    val newRatePlan = PaidTierPlan(newTier, annual)

    for {
      subscriptionResult <- touchpointBackend.subscriptionService.upgradeSubscription(member, newRatePlan, preview = false)
      memberId <- touchpointBackend.memberRepository.upsert(member.identityId, memberData(newRatePlan, customerOpt))
    } yield {
      form.map(IdentityService(IdentityApi).updateUserFieldsBasedOnUpgrade(user, _, identityRequest))
      touchpointBackend.memberRepository.metrics.putUpgrade(newTier)
      memberId
    }
  }

  def previewUpgradeSubscription(paidMember: PaidMember, user: IdMinimalUser, newTier: Tier): Future[Seq[PreviewInvoiceItem]] = {
    val touchpointBackend = TouchpointBackend.forUser(user)

    for {
      paymentSummary <- touchpointBackend.subscriptionService.getPaymentSummary(paidMember)
      newRatePlan = PaidTierPlan(newTier, paymentSummary.current.annual)
      subscriptionResult <- touchpointBackend.subscriptionService.upgradeSubscription(paidMember, newRatePlan, preview = true)
    } yield subscriptionResult.invoiceItems
  }

}

object MemberService extends MemberService
