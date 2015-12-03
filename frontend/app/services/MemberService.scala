package services

import com.gu.identity.play.{IdMinimalUser, IdUser}
import com.gu.membership.model._
import com.gu.membership.salesforce.ContactDeserializer.Keys
import com.gu.membership.salesforce._
import com.gu.membership.salesforce.Contact._
import com.gu.membership.stripe.Stripe
import com.gu.membership.stripe.Stripe.Customer
import com.gu.membership.util.{FutureSupplier, Timing}
import com.gu.membership.zuora.soap.Readers._
import com.gu.membership.zuora.soap.actions.Actions.CreateFreeEventUsage
import com.gu.membership.zuora.soap.models.{SubscriptionDetails, PreviewInvoiceItem}
import com.gu.membership.zuora.soap.models.Results.CreateResult
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import controllers.IdentityRequest
import forms.MemberForm._
import model.Benefits.DiscountTicketTiers
import model.Eventbrite.{EBCode, EBOrder, EBTicketClass}
import model.FreeEventTickets
import model.RichEvent._
import monitoring.MemberMetrics
import org.joda.time.Period
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.json.Json
import services.EventbriteService._
import tracking._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

case class MemberServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}

class FrontendMemberRepository(salesforceConfig: SalesforceConfig) extends ContactRepository {
  val metrics = new MemberMetrics(salesforceConfig.envName)

  val salesforce = new Scalaforce {
    val consumerKey = salesforceConfig.consumerKey
    val consumerSecret = salesforceConfig.consumerSecret

    val apiURL = salesforceConfig.apiURL.toString()
    val apiUsername = salesforceConfig.apiUsername
    val apiPassword = salesforceConfig.apiPassword
    val apiToken = salesforceConfig.apiToken

    val stage = Config.stage
    val application = "Frontend"

    override val authSupplier: FutureSupplier[Authentication] = new FutureSupplier[Authentication](getAuthentication)

    private val actorSystem = Akka.system
    actorSystem.scheduler.schedule(30.minutes, 30.minutes) { authSupplier.refresh() }
  }

  def getMember(userId: String): Future[Option[Contact[Member, PaymentMethod]]] =
    get(userId).map(_.collect { case Contact(d, m: Member, p) => Contact(d, m, p) })
}

trait MemberService extends LazyLogging with ActivityTracking {

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

  def memberData(plan: TierPlan, customerOpt: Option[Stripe.Customer]) = Json.obj(
    Keys.TIER -> plan.salesforceTier
  ) ++ customerOpt.map { customer =>
    Json.obj(
      Keys.STRIPE_CUSTOMER_ID -> customer.id,
      Keys.DEFAULT_CARD_ID -> customer.card.id
    )
  }.getOrElse(Json.obj())

  def createMember(user: IdMinimalUser,
                   formData: JoinForm,
                   identityRequest: IdentityRequest,
                   campaignCode: Option[String] = None): Future[ContactId] = {

    val tp = TouchpointBackend.forUser(user)
    val identityService = IdentityService(IdentityApi)

    Timing.record(tp.memberRepository.metrics, "createMember") {
      def futureCustomerOpt = formData match {
        case paid: PaidMemberJoinForm => tp.stripeService.Customer.create(user.id, paid.payment.token).map(Some(_))
        case _ => Future.successful(None)
      }

      formData.password.foreach(identityService.updateUserPassword(_, identityRequest, user.id))

      val casId = formData match {
        case paidMemberJoinForm: PaidMemberJoinForm => paidMemberJoinForm.casId
        case _ => None
      }

      for {
        fullUser <- identityService.getFullUserDetails(user, identityRequest)
        customerOpt <- futureCustomerOpt
        userData = initialData(fullUser, formData)
        memberId <- tp.memberRepository.upsert(user.id, userData)
        subscription <- tp.subscriptionService.createSubscription(memberId, formData, customerOpt)

        // Set some fields once subscription has been successful
        updatedMember <- tp.memberRepository.upsert(user.id, memberData(formData.plan, customerOpt))
      } yield {
        identityService.updateUserFieldsBasedOnJoining(user, formData, identityRequest)

        tp.memberRepository.metrics.putSignUp(formData.plan)
        trackRegistration(formData, memberId, user, campaignCode)
        memberId
      }
    }.andThen {
      case Success(memberAccount) => logger.debug(s"createMember() success user=${user.id} memberAccount=$memberAccount")
      case Failure(error: Stripe.Error) => logger.warn(s"Stripe API call returned error: '${error.getMessage()}' for user ${user.id}")
      case Failure(error) =>
        logger.error(s"Error in createMember() user=${user.id}", error)
        tp.memberRepository.metrics.putFailSignUp(formData.plan)
    }
  }

  def countComplimentaryTicketsInOrder(event: RichEvent, order: EBOrder): Int = {
    val ticketIds = event.internalTicketing.map(_.complimentaryTickets).getOrElse(Nil).map(_.id)
    order.attendees.count(attendee => ticketIds.contains(attendee.ticket_class_id))
  }

  def recordFreeEventUsage(member: Contact[Member, PaymentMethod], event: RichEvent, order: EBOrder, quantity: Int): Future[CreateResult] = {
    val tp = TouchpointBackend.forUser(member)
    for {
      (account, subscription) <- tp.subscriptionService.accountWithLatestMembershipSubscription(member)
      description = s"event-id:${event.id};order-id:${order.id}"
      action = CreateFreeEventUsage(account.id, description, quantity, subscription.subscriptionNumber)
      result <- tp.zuoraSoapClient.authenticatedRequest(action)
    } yield {
      logger.info(s"Recorded a complimentary event ticket usage for account ${account.id}, subscription: ${subscription.subscriptionNumber}, details: $description")
      result
    }
  }

  def retrieveComplimentaryTickets(member: Contact[Member, PaymentMethod], event: RichEvent): Future[Seq[EBTicketClass]] = {
    val tp = TouchpointBackend.forUser(member)
    Timing.record(tp.memberRepository.metrics, "retrieveComplimentaryTickets") {
      for {
        (_, subscription) <- tp.subscriptionService.accountWithLatestMembershipSubscription(member)
        usageCount <- tp.subscriptionService.getUsageCountWithinTerm(subscription, FreeEventTickets.unitOfMeasure)
      } yield {
        val hasComplimentaryTickets = usageCount.isDefined
        val allowanceNotExceeded = usageCount.exists(_ < FreeEventTickets.allowance)
        logger.info(
          s"User ${member.identityId} has used $usageCount tickets" ++
            s"(allowance not exceeded: $allowanceNotExceeded, is entitled: $hasComplimentaryTickets)")

        if (hasComplimentaryTickets && allowanceNotExceeded)
          event.internalTicketing.map(_.complimentaryTickets).getOrElse(Nil)
        else Nil
      }
    }
  }

  def retrieveDiscountedTickets(member: Contact[Member, PaymentMethod], event: RichEvent): Seq[EBTicketClass] = {
    (for {
      ticketing <- event.internalTicketing
      benefit <- ticketing.memberDiscountOpt if DiscountTicketTiers.contains(member.memberStatus.tier)
    } yield ticketing.memberBenefitTickets)
      .getOrElse(Seq[EBTicketClass]())
  }

  def createEBCode(member: Contact[Member, PaymentMethod], event: RichEvent): Future[Option[EBCode]] = {
    retrieveComplimentaryTickets(member, event).flatMap { complimentaryTickets =>
      val code = DiscountCode.generate(s"A_${member.identityId}_${event.id}")
      val unlockedTickets = complimentaryTickets ++ retrieveDiscountedTickets(member, event)
      event.service.createOrGetAccessCode(event, code, unlockedTickets)
    }
  }

  def previewUpgradeSubscription(subscriptionDetails: SubscriptionDetails,
                                 contactId: ContactId,
                                 newTier: PaidTier,
                                 tp: TouchpointBackend): Future[Seq[PreviewInvoiceItem]] =
    for {
      cat <- tp.catalog
      currentPlan = cat.unsafePaidTierPlan(subscriptionDetails.productRatePlanId)
      newPlan = PaidTierPlan(newTier, currentPlan.billingPeriod, Current)
      subscriptionResult <- tp.subscriptionService.upgradeSubscription(contactId, newPlan, preview = true, Set.empty)
    } yield subscriptionResult.invoiceItems

  def upgradeFreeSubscription(freeMember: Contact[Member, NoPayment],
                              newTier: PaidTier,
                              form: FreeMemberChangeForm,
                              identityRequest: IdentityRequest,
                              campaignCode: Option[String]): Future[ContactId] = {

    val touchpointBackend = TouchpointBackend.forUser(freeMember)
    val plan = PaidTierPlan(newTier, form.payment.billingPeriod, Current)
    for {
      customer <- touchpointBackend.stripeService.Customer.create(freeMember.identityId, form.payment.token)
      paymentResult <- touchpointBackend.subscriptionService.createPaymentMethod(freeMember, customer)
      memberId <- upgradeSubscription(freeMember, plan, form, Some(customer), identityRequest, campaignCode)
    } yield {

      memberId
    }
  }

  def upgradePaidSubscription(paidMember: Contact[Member, StripePayment],
                              newTier: PaidTier,
                              identityRequest: IdentityRequest,
                              campaignCode: Option[String],
                              form: PaidMemberChangeForm): Future[ContactId] = {

    val tp = TouchpointBackend.forUser(paidMember)
    val catalog = tp.catalog
    for {
      subs <- tp.subscriptionService.getCurrentSubscriptionDetails(paidMember)
      cat <- catalog
      currentPlan = cat.unsafePaidTierPlan(subs.productRatePlanId)
      newPlan = PaidTierPlan(newTier, currentPlan.billingPeriod, status = Current)
      memberId <- upgradeSubscription(paidMember, newPlan, form, None, identityRequest, campaignCode)
    } yield memberId

  }

  private def upgradeSubscription(member: Contact[Member, PaymentMethod],
                                  newRatePlan: PaidTierPlan,
                                  form: MemberChangeForm,
                                  customerOpt: Option[Customer],
                                  identityRequest: IdentityRequest,
                                  campaignCode: Option[String]): Future[ContactId] = {

    val touchpointBackend = TouchpointBackend.forUser(member)
    val addressDetails = form.addressDetails

    addressDetails.foreach(
      IdentityService(IdentityApi).updateUserFieldsBasedOnUpgrade(member.identityId, _, identityRequest))

    for {
      subscriptionResult <- touchpointBackend.subscriptionService.upgradeSubscription(member, newRatePlan, preview = false, form.featureChoice)
      memberId <- touchpointBackend.memberRepository.upsert(member.identityId, memberData(newRatePlan, customerOpt))
    } yield {
      touchpointBackend.memberRepository.metrics.putUpgrade(newRatePlan.tier)
      trackUpgrade(memberId, member, newRatePlan, campaignCode, addressDetails)
      memberId
    }
  }

  private def trackUpgrade(memberId: ContactId,
                           member: Contact[Member, PaymentMethod],
                           newRatePlan: PaidTierPlan,
                           campaignCode: Option[String],
                           addressDetails: Option[AddressDetails]): Unit = {

    track(
      MemberActivity(source = "membershipUpgrade",
        MemberData(
          salesforceContactId = memberId.salesforceContactId,
          identityId = member.identityId,
          tier = member.tier.name,
          tierAmendment = Some(UpgradeAmendment(member.tier, newRatePlan.tier)),
          deliveryPostcode = addressDetails.map(_.deliveryAddress.postCode),
          billingPostcode = addressDetails.flatMap(f => f.billingAddress.map(_.postCode)).orElse(addressDetails.map(_.deliveryAddress.postCode)),
          subscriptionPaymentAnnual = Some(newRatePlan.billingPeriod.annual),
          marketingChoices = None,
          city = addressDetails.map(_.deliveryAddress.town),
          country = addressDetails.map(_.deliveryAddress.country.name),
          campaignCode=campaignCode
        )),
      member)
  }

  private def trackRegistration(formData: JoinForm, member: ContactId, user: IdMinimalUser, campaignCode: Option[String] = None) {
    val subscriptionPaymentAnnual = formData match {
      case paidMemberJoinForm: PaidMemberJoinForm => Some(paidMemberJoinForm.payment.billingPeriod.annual)
      case _ => None
    }

    val billingPostcode = formData match {
      case paidMemberJoinForm: PaidMemberJoinForm =>
        paidMemberJoinForm.billingAddress.map(_.postCode).orElse(Some(formData.deliveryAddress.postCode))
      case _ => None
    }

    val trackingInfo =
      MemberData(
        member.salesforceContactId,
        user.id,
        formData.plan.salesforceTier,
        None,
        Some(formData.deliveryAddress.postCode),
        billingPostcode,
        subscriptionPaymentAnnual,
        Some(formData.marketingChoices),
        Some(formData.deliveryAddress.town),
        Some(formData.deliveryAddress.country.name),
        campaignCode
    )

    track(MemberActivity("membershipRegistration", trackingInfo), user)
  }
}

object MemberService extends MemberService
