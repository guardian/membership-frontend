package services

import com.gu.identity.model.User
import com.gu.membership.salesforce.Member.Keys
import com.gu.membership.salesforce._
import com.gu.membership.util.Timing
import configuration.Config
import controllers.IdentityRequest
import forms.MemberForm._
import model.Eventbrite.{EBCode, RichEvent}
import model.PaidTierPlan
import model.Stripe.Customer
import monitoring.TouchpointBackendMetrics
import utils.ScheduledTask

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

case class MemberServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}

class FrontendMemberRepository(salesforceConfig: SalesforceConfig) extends MemberRepository with ScheduledTask[Authentication] {
  val metrics = new TouchpointBackendMetrics {

    override val backendEnv = salesforceConfig.envName

    val service = "Member"

    def putSignUp(tier: Tier.Tier) {
      put(s"sign-ups-${tier.toString}")
    }

    def putUpgrade(tier: Tier.Tier) {
      put(s"upgrade-${tier.toString}")
    }

    def putDowngrade(tier:Tier.Tier) {
      put(s"downgrade-${tier.toString}")
    }

    def putCancel(tier:Tier.Tier) {
      put(s"cancel-${tier.toString}")
    }

    private def put(metricName: String) {
      put(metricName, 1)
    }
  }

  val initialValue = Authentication("", "")
  val initialDelay = 0.seconds
  val interval = 30.minutes

  def refresh() = salesforce.getAuthentication

  val salesforce = new Scalaforce {
    val consumerKey = salesforceConfig.consumerKey
    val consumerSecret = salesforceConfig.consumerSecret

    val apiURL = salesforceConfig.apiURL.toString
    val apiUsername = salesforceConfig.apiUsername
    val apiPassword = salesforceConfig.apiPassword
    val apiToken = salesforceConfig.apiToken

    val stage = Config.stage
    val application = "Frontend"

    def authentication: Authentication = agent.get()
  }
}

trait MemberService {
  def initialData(user: User, formData: JoinForm) = Map(
    Keys.EMAIL -> user.getPrimaryEmailAddress,
    Keys.FIRST_NAME -> formData.name.first,
    Keys.LAST_NAME -> formData.name.last,
    Keys.MAILING_STREET -> formData.deliveryAddress.line,
    Keys.MAILING_CITY -> formData.deliveryAddress.town,
    Keys.MAILING_STATE -> formData.deliveryAddress.countyOrState,
    Keys.MAILING_POSTCODE -> formData.deliveryAddress.postCode,
    Keys.MAILING_COUNTRY -> formData.deliveryAddress.country.alpha2,
    Keys.ALLOW_MEMBERSHIP_MAIL -> true
  ) ++
    formData.marketingChoices.thirdParty.map( Keys.ALLOW_THIRD_PARTY_EMAIL -> _) ++
    formData.marketingChoices.gnm.map( Keys.ALLOW_GU_RELATED_MAIL -> _)

  def memberData(tier: Tier.Tier, customerOpt: Option[Customer]) = Map(
    Keys.TIER -> tier.toString
  ) ++ customerOpt.map { customer =>
    Map(
      Keys.CUSTOMER_ID -> customer.id,
      Keys.DEFAULT_CARD_ID -> customer.card.id
    )
  }.getOrElse(Map.empty)

  def createMember(user: User, formData: JoinForm, identityRequest: IdentityRequest): Future[String] = {
    val touchpointBackend = TouchpointBackend.forUser(user)

    Timing.record(touchpointBackend.memberRepository.metrics, "createMember") {
      def futureCustomerOpt = formData match {
        case paid: PaidMemberJoinForm => touchpointBackend.stripeService.Customer.create(user.id, paid.payment.token).map(Some(_))
        case friend: FriendJoinForm => Future.successful(None)
      }

      formData.password.map(IdentityService.updateUserPassword(_, identityRequest, user.id))

      for {
        customerOpt <- futureCustomerOpt
        memberId <- touchpointBackend.memberRepository.upsert(user.id, initialData(user, formData))
        subscription <- touchpointBackend.subscriptionService.createSubscription(memberId, formData, customerOpt)

        // Set some fields once subscription has been successful
        updatedMember <- touchpointBackend.memberRepository.upsert(user.id, memberData(formData.tierPlan.tier, customerOpt))
      } yield {
        IdentityService.updateUserFieldsBasedOnJoining(user, formData, identityRequest)

        touchpointBackend.memberRepository.metrics.putSignUp(formData.tierPlan.tier)
        memberId.account
      }
    }
  }

  def createDiscountForMember(member: Member, event: RichEvent): Future[Option[EBCode]] = {
    member.tier match {
      case Tier.Friend => Future.successful(None)

      case _ =>
        if (event.memberTickets.nonEmpty) {
          // Add a "salt" to make access codes different to discount codes
          val code = DiscountCode.generate(s"A_${member.identityId}_${event.id}")
          GuardianLiveEventService.createOrGetAccessCode(event, code, event.memberTickets).map(Some(_))
        } else if (event.allowDiscountCodes) {
          val code = DiscountCode.generate(s"${member.identityId}_${event.id}")
          GuardianLiveEventService.createOrGetDiscount(event, code).map(Some(_))
        } else {
          Future.successful(None)
        }
    }
  }

  // TODO: this currently only handles free -> paid
  def upgradeSubscription(member: FreeMember, user: User, newTier: Tier.Tier, form: PaidMemberChangeForm, identityRequest: IdentityRequest): Future[String] = {
    val touchpointBackend = TouchpointBackend.forUser(user)
    for {
      customer <- touchpointBackend.stripeService.Customer.create(user.id, form.payment.token)
      paymentResult <- touchpointBackend.subscriptionService.createPaymentMethod(member.salesforceAccountId, customer)
      subscriptionResult <- touchpointBackend.subscriptionService.upgradeSubscription(member.salesforceAccountId, PaidTierPlan(newTier, form.payment.annual))
      memberId <- touchpointBackend.memberRepository.upsert(member.identityId, memberData(newTier, Some(customer)))
    } yield {
      IdentityService.updateUserFieldsBasedOnUpgrade(user, form, identityRequest)
      touchpointBackend.memberRepository.metrics.putUpgrade(newTier)
      memberId.account
    }
  }
}

object MemberService extends MemberService
