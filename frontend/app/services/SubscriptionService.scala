package services

import com.gu.config.Membership
import com.gu.i18n.{Country, CountryGroup, Currency, GBP}
import com.gu.identity.play.{IdMinimalUser, IdUser}
import com.gu.membership.model._
import com.gu.membership.salesforce.ContactDeserializer.Keys
import com.gu.membership.salesforce.Tier.{Partner, Patron}
import com.gu.membership.salesforce.{Member => _, PaymentMethod => _, _}
import com.gu.membership.stripe.Stripe
import com.gu.membership.stripe.Stripe.Customer
import com.gu.membership.touchpoint.TouchpointBackendConfig.BackendType
import com.gu.membership.util.{FutureSupplier, Timing}
import com.gu.membership.zuora.soap.{AndFilter, OrFilter, DateTimeHelpers, SimpleFilter}
import com.gu.membership.zuora.soap.actions.Actions._
import com.gu.membership.zuora.soap.actions.subscribe.{Account => SoapSubscribeAccount, CreditCardReferenceTransaction, Subscribe}
import com.gu.membership.zuora.soap.models.Queries.PreviewInvoiceItem
import com.gu.membership.zuora.soap.models.Results.{UpdateResult, AmendResult, SubscribeResult, CreateResult}
import com.gu.membership.zuora.soap.models.{Queries => SoapQueries, PaymentSummary, SubscriptionStatus}
import com.gu.membership.zuora.{rest, soap}
import com.gu.membership.{salesforce => sf}
import com.gu.monitoring.ServiceMetrics
import com.gu.services.PaymentService
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import controllers.IdentityRequest
import forms.MemberForm._
import model.Eventbrite.{EBCode, EBOrder, EBTicketClass}
import model.RichEvent.RichEvent
import model.RichEvent._
import model._
import monitoring.MemberMetrics
import org.joda.time.DateTime
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.json._
import tracking._
import views.support.ThankyouSummary
import views.support.ThankyouSummary.NextPayment
import services.EventbriteService._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future
import scala.util.{Failure, Success}
case class MemberServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}

// --------------- SALESFORCE ----------------------
class FrontendMemberRepository(salesforceConfig: SalesforceConfig) extends ContactRepository {
  import scala.concurrent.duration._
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

    override val authSupplier: FutureSupplier[sf.Authentication] = new FutureSupplier[sf.Authentication](getAuthentication)

    private val actorSystem = Akka.system
    actorSystem.scheduler.schedule(30.minutes, 30.minutes) { authSupplier.refresh() }
  }

  def getMember(userId: String): Future[Option[SFMember]] =
    get(userId).map(_.collect { case Contact(d, m: sf.Member, p) => Contact(d, m, p) })
}

trait MemberService extends LazyLogging with ActivityTracking {
  def initialData(user: IdUser, formData: JoinForm): JsObject = {
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

  def memberData(plan: TierPlan, customerOpt: Option[Stripe.Customer]): JsObject = Json.obj(
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
                   fromEventId: Option[String]): Future[ContactId] = {

    val tp = TouchpointBackend.forUser(user)
    val identityService = IdentityService(IdentityApi)
    val tier = formData.plan.tier

    val createContact: Future[ContactId] =
      for {
        user <- identityService.getFullUserDetails(user, identityRequest)
        userData = initialData(user, formData)
        contactId <- tp.memberRepository.upsert(user.id, userData)
      } yield contactId

    def updateContact(customer: Option[Customer]) =
      tp.memberRepository.upsert(user.id, memberData(formData.plan, customer))

    Timing.record(tp.memberRepository.metrics, "createMember") {
      formData.password.foreach(identityService.updateUserPassword(_, identityRequest, user.id))

      val contactId = formData match {
        case paid: PaidMemberJoinForm =>
          for {
            customer <- tp.stripeService.Customer.create(user.id, paid.payment.token)
            cId <- createContact
            subscription <- tp.subscriptionService.createPaidSubscription(cId, paid, customer)
            updatedMember <- updateContact(Some(customer))
          } yield cId
        case _ =>
          for {
            cId <- createContact
            subscription <- tp.subscriptionService.createFreeSubscription(cId, formData)
            updatedMember <- updateContact(None)
          } yield cId
      }

      contactId.map { cId =>
        identityService.updateUserFieldsBasedOnJoining(user, formData, identityRequest)

        tp.memberRepository.metrics.putSignUp(formData.plan)
        trackRegistration(formData, cId, user)
        cId
      }
    }.andThen {
      case Success(contactId) =>
        logger.debug(s"createMember() success user=${user.id} memberAccount=$contactId")
        fromEventId.flatMap(EventbriteService.getBookableEvent).foreach { event =>
          event.service.wsMetrics.put(s"join-${tier.name}-event", 1)
          val memberData = MemberData(contactId.salesforceContactId, user.id, tier.name)
          track(EventActivity("membershipRegistrationViaEvent", Some(memberData), EventData(event)), user)
        }
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

  def recordFreeEventUsage(member: SFMember, event: RichEvent, order: EBOrder, quantity: Int): Future[CreateResult] = {
    val tp = TouchpointBackend.forUser(member)
    for {
      subs <- tp.subscriptionService.currentSubscription(member)
      description = s"event-id:${event.id};order-id:${order.id}"
      action = CreateFreeEventUsage(subs.accountId, description, quantity, subs.number)
      result <- tp.zuoraSoapClient.authenticatedRequest(action)
    } yield {
      logger.info(s"Recorded a complimentary event ticket usage for account ${subs.accountId}, subscription: ${subs.number}, details: $description")
      result
    }
  }

  def retrieveComplimentaryTickets(member: SFMember, event: RichEvent): Future[Seq[EBTicketClass]] = {

    val tp = TouchpointBackend.forUser(member)
    Timing.record(tp.memberRepository.metrics, "retrieveComplimentaryTickets") {
      for {
        subs <- tp.subscriptionService.currentSubscription(member)
        usageCount <- tp.subscriptionService.getUsageCountWithinTerm(subs, FreeEventTickets.unitOfMeasure)
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

  def retrieveDiscountedTickets(member: SFMember, event: RichEvent): Seq[EBTicketClass] = {
    (for {
      ticketing <- event.internalTicketing
      benefit <- ticketing.memberDiscountOpt if Benefits.DiscountTicketTiers.contains(member.memberStatus.tier)
    } yield ticketing.memberBenefitTickets)
      .getOrElse(Seq[EBTicketClass]())
  }

  def createEBCode(member: SFMember, event: RichEvent): Future[Option[EBCode]] = {
    retrieveComplimentaryTickets(member, event).flatMap { complimentaryTickets =>
      val code = DiscountCode.generate(s"A_${member.identityId}_${event.id}")
      val unlockedTickets = complimentaryTickets ++ retrieveDiscountedTickets(member, event)
      event.service.createOrGetAccessCode(event, code, unlockedTickets)
    }
  }

  def previewUpgradeSubscription(subscription: model.Subscription,
                                 contact: SFMember,
                                 newTier: PaidTier): Future[Seq[PreviewInvoiceItem]] = {
    val tp = TouchpointBackend.forUser(contact)
    for {
      cat <- tp.catalog
      currentPlan = cat.unsafePaidTierPlan(subscription.productRatePlanId)
      newPlan = PaidTierPlan(newTier, currentPlan.billingPeriod, Current)
      subscriptionResult <- tp.subscriptionService.upgradeSubscription(contact, newPlan, preview = true, Set.empty)
    } yield subscriptionResult.invoiceItems
  }

  def upgradeFreeSubscription(freeMember: NonPaidSFMember,
                              newTier: PaidTier,
                              form: FreeMemberChangeForm,
                              identityRequest: IdentityRequest): Future[ContactId] = {

    val touchpointBackend = TouchpointBackend.forUser(freeMember)
    val plan = PaidTierPlan(newTier, form.payment.billingPeriod, Current)
    for {
      customer <- touchpointBackend.stripeService.Customer.create(freeMember.identityId, form.payment.token)
      paymentResult <- touchpointBackend.subscriptionService.createPaymentMethod(freeMember, customer)
      memberId <- upgradeSubscription(freeMember, plan, form, Some(customer), identityRequest)
    } yield {

      memberId
    }
  }

  def upgradePaidSubscription(paidMember: PaidSFMember,
                              newTier: PaidTier,
                              identityRequest: IdentityRequest,
                              form: PaidMemberChangeForm): Future[ContactId] = {

    val tp = TouchpointBackend.forUser(paidMember)
    val catalog = tp.catalog
    for {
      subs <- tp.subscriptionService.currentPaidSubscription(paidMember)
      cat <- catalog
      currentPlan = cat.unsafePaidTierPlan(subs.productRatePlanId)
      newPlan = PaidTierPlan(newTier, currentPlan.billingPeriod, status = Current)
      memberId <- upgradeSubscription(paidMember, newPlan, form, None, identityRequest)
    } yield memberId

  }

  private def upgradeSubscription(member: SFMember,
                                  newRatePlan: PaidTierPlan,
                                  form: MemberChangeForm,
                                  customerOpt: Option[Customer],
                                  identityRequest: IdentityRequest): Future[ContactId] = {

    val touchpointBackend = TouchpointBackend.forUser(member)
    val addressDetails = form.addressDetails

    addressDetails.foreach(
      IdentityService(IdentityApi).updateUserFieldsBasedOnUpgrade(member.identityId, _, identityRequest))

    for {
      subscriptionResult <- touchpointBackend.subscriptionService.upgradeSubscription(member, newRatePlan, preview = false, form.featureChoice)
      memberId <- touchpointBackend.memberRepository.upsert(member.identityId, memberData(newRatePlan, customerOpt))
    } yield {
      touchpointBackend.memberRepository.metrics.putUpgrade(newRatePlan.tier)
      trackUpgrade(memberId, member, newRatePlan, addressDetails)
      memberId
    }
  }

  private def trackUpgrade(memberId: ContactId,
                           member: SFMember,
                           newRatePlan: PaidTierPlan,
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
          country = addressDetails.map(_.deliveryAddress.country.name)
        )),
      member)
  }

  private def trackRegistration(formData: JoinForm, member: ContactId, user: IdMinimalUser) {
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
        Some(formData.deliveryAddress.country.name)
      )

    track(MemberActivity("membershipRegistration", trackingInfo), user)
  }

  def getStripeCustomer(contact: GenericSFContact): Future[Option[Customer]] = contact.paymentMethod match {
    case StripePayment(id) =>
      TouchpointBackend.forUser(contact).stripeService.Customer.read(id).map(Some(_))
    case _ =>
      Future.successful(None)
  }
}

object MemberService extends MemberService

case class SubscriptionServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}

object SubscriptionService {
  val membershipProductType = "Membership"
  val productRatePlanChargeModel = "FlatFee"

  /**
   * A Zuora subscription may have many versions as it is amended, some of which can be in the future (ie. downgrading
   * from a paid tier - because we don't refund that user, the downgrade is instead set to the point in the future when
   * their paid period ends).
   *
   * The Zuora API does not explicitly tell you what the *current* subscription version is. You have to work it out,
   * by looking at the 'amendments', finding the first amendment that has yet occurred. That amendment will give you the
   * id of the subscription it modified - and THAT will be the *current* subscription version.
   */
  def findCurrentSubscriptionStatus(subscriptionVersions: Seq[SoapQueries.Subscription], amendments: Seq[SoapQueries.Amendment]): SubscriptionStatus = {
    val firstAmendmentWhichHasNotYetOccurredOpt = // this amendment *will have modified the current subscription*
      sortAmendments(subscriptionVersions, amendments).find(_.contractEffectiveDate.isAfterNow)

    val latestSubVersion = subscriptionVersions.maxBy(_.version)

    firstAmendmentWhichHasNotYetOccurredOpt.fold(SubscriptionStatus(latestSubVersion, None, None)) { amendmentOfCurrentSub =>
      val currentSubId = amendmentOfCurrentSub.subscriptionId
      val currentSubVersion = subscriptionVersions.find(_.id == currentSubId).get
      SubscriptionStatus(currentSubVersion, Some(latestSubVersion), Some(amendmentOfCurrentSub.amendType))
    }
  }

  /**
   * Given an array of subscription invoice items, return only items corresponding to the last invoice
   * @param items The incoming list of items with many invoices, potentially associated to multiple subscription versions
   */
  def latestInvoiceItems(items: Seq[SoapQueries.InvoiceItem]): Seq[SoapQueries.InvoiceItem] = {
    if(items.isEmpty)
      items
    else {
      val sortedItems = items.sortBy(_.chargeNumber)
      sortedItems.filter(_.subscriptionId == sortedItems.last.subscriptionId)
    }
  }

  /**
   * @param amendments which are returned by the Zurora API in an unpredictable order
   * @return amendments which are sorted by the subscription version number they point to (the sub they amended)
   */
  def sortAmendments(subscriptions: Seq[SoapQueries.Subscription], amendments: Seq[SoapQueries.Amendment]): Seq[SoapQueries.Amendment] = {
    val versionsNumberBySubVersionId = subscriptions.map { sub => (sub.id, sub.version) }.toMap
    amendments.sortBy { amendment => versionsNumberBySubVersionId(amendment.subscriptionId) }
  }

  def sortPreviewInvoiceItems(items: Seq[SoapQueries.PreviewInvoiceItem]): Seq[PreviewInvoiceItem] = items.sortBy(_.price)

  def sortSubscriptions(subscriptions: Seq[SoapQueries.Subscription]): Seq[SoapQueries.Subscription] = subscriptions.sortBy(_.version)

  def featuresPerTier(zuoraFeatures: Seq[SoapQueries.Feature])(plan: TierPlan, choice: Set[FeatureChoice]): Seq[SoapQueries.Feature] = {
    def byChoice(choice: Set[FeatureChoice]) =
      zuoraFeatures.filter(f => choice.map(_.zuoraCode).contains(f.code))

    plan.tier match {
      case Patron => byChoice(FeatureChoice.all)
      case Partner => byChoice(choice).take(1)
      case _ => Nil
    }
  }

  def supportedAccountCurrency(catalog: MembershipCatalog)(country: Country, plan: PaidTierPlan): Currency =
    CountryGroup
      .byCountryCode(country.alpha2).map(_.currency)
      .filter(catalog.paidTierPlanDetails(plan).currencies)
      .getOrElse(GBP)
}

class SubscriptionService(val zuoraSoapClient: soap.ClientWithFeatureSupplier,
			                    val zuoraRestClient: rest.Client,
			                    val metrics: ServiceMetrics,
			                    val productFamily: Membership,
			                    val bt: BackendType,
			                    val paymentService: PaymentService) extends LazyLogging with ActivityTracking {

  import SubscriptionService._

  implicit private val _bt = bt

  val membershipCatalog: FutureSupplier[MembershipCatalog] = new FutureSupplier[MembershipCatalog](
    productRatePlans.map(MembershipCatalog.unsafeFromZuora(productFamily))
  )

  def productRatePlans: Future[Seq[rest.ProductRatePlan]] =
    zuoraRestClient.productCatalog.map(_.products.flatMap(_.productRatePlans))

  def getMembershipCatalog: Future[MembershipCatalog.Val[MembershipCatalog]] =
    productRatePlans.map(MembershipCatalog.fromZuora(productFamily))

  private def subscriptionVersions(subscriptionNumber: String): Future[Seq[SoapQueries.Subscription]] = for {
    subscriptions <- zuoraSoapClient.query[SoapQueries.Subscription](SimpleFilter("Name", subscriptionNumber))
  } yield subscriptions

  def currentSubscription(contact: ContactId): Future[model.Subscription] = for {
    catalog <- membershipCatalog.get()
    accounts <- zuoraSoapClient.query[SoapQueries.Account](SimpleFilter("crmId", contact.salesforceAccountId))
    accountAndSubscriptionOpts <- Future.traverse(accounts) { account =>
      zuoraRestClient.latestSubscriptionOpt(productFamily.productRatePlanIds, Set(account.id)).map(account -> _)
    }
  } yield {
    val (account, restSub) =
      accountAndSubscriptionOpts.collect { case (acc, Some(subscription)) =>
        acc -> subscription
      }.sortBy(_._2.termStartDate).lastOption.getOrElse(throw new SubscriptionServiceError(
        s"Cannot find a membership subscription for account ids ${accounts.map(_.id)}"))

     model.Subscription(catalog)(contact, account, restSub)
    }

  def currentPaidSubscription(contact: ContactId): Future[model.PaidSubscription] =
    currentSubscription(contact).map {
      case paid:PaidSubscription => paid
      case sub =>
        throw SubscriptionServiceError(s"Expecting subscription ${sub.number} to be paid, got a free one instead (tier: ${sub.plan})")
    }


  /**
   * @return the current and the future subscription version of the user if
   *         they have a pending amendment (Currently this is the case only of downgrades, as upgrades
   *         are effective immediately)
   */
  def getSubscriptionStatus(memberId: ContactId): Future[SubscriptionStatus] =
    currentSubscription(memberId).flatMap(sub => getSubscriptionStatus(sub.number))

  def getSubscriptionStatus(subscriptionNumber: String): Future[SubscriptionStatus] = for {
    subscriptionVersions <- subscriptionVersions(subscriptionNumber)
    amendments <- zuoraSoapClient.query[SoapQueries.Amendment](OrFilter(subscriptionVersions.map(s => ("SubscriptionId", s.id)): _*))
  } yield findCurrentSubscriptionStatus(subscriptionVersions, amendments)

  /*
   * If the member is entitled to complimentary tickets return its Zuora account's corresponding usage records count.
   * Returns none otherwise
   */
  def getUsageCountWithinTerm(subscription: model.Subscription, unitOfMeasure: String): Future[Option[Int]] = {
    val features = subscription.features
    //TODO: review date formats here
    val startDate = DateTimeHelpers.formatDateTime(subscription.startDate.toDateTimeAtCurrentTime)

    val usageCountF = zuoraSoapClient.query[SoapQueries.Usage](AndFilter(("StartDateTime", startDate),
							                                                    ("SubscriptionNumber", subscription.number),
							                                                    ("UOM", unitOfMeasure))).map(_.size)
    for {
      usageCount <- usageCountF
    } yield {
      val hasComplimentaryTickets = features.contains(FreeEventTickets)
      if (!hasComplimentaryTickets) None else Some(usageCount)
    }
  }

  def createPaymentMethod(memberId: ContactId, customer: Stripe.Customer): Future[UpdateResult] = for {
    sub <- currentSubscription(memberId)
    paymentMethod <- zuoraSoapClient.authenticatedRequest(
      CreateCreditCardReferencePaymentMethod(sub.accountId, customer.card.id, customer.id))
    result <- zuoraSoapClient.authenticatedRequest(EnablePayment(sub.accountId, paymentMethod.id))
  } yield result

  def createFreeSubscription(memberId: ContactId, joinData: JoinForm): Future[SubscribeResult] =
    for {
      zuoraFeatures <- zuoraSoapClient.featuresSupplier.get()
      ratePlanId <- findRatePlanId(joinData.plan)
      result <- zuoraSoapClient.authenticatedRequest(Subscribe(
        account = SoapSubscribeAccount.stripe(memberId, GBP, autopay = false),
        paymentMethodOpt = None,
        ratePlanId = ratePlanId,
        firstName = joinData.name.first,
        lastName = joinData.name.last,
        address = joinData.deliveryAddress,
        casIdOpt = None,
        paymentDelay = None,
        ipAddressOpt = None,
        featureIds = Nil))
    } yield result

  def createPaidSubscription(memberId: ContactId,
                             joinData: PaidMemberJoinForm,
                             customer: Stripe.Customer): Future[SubscribeResult] =
    for {
      catalog <- membershipCatalog.get()
      zuoraFeatures <- zuoraSoapClient.featuresSupplier.get()
      ratePlanId <- findRatePlanId(joinData.plan)
      result <- zuoraSoapClient.authenticatedRequest(Subscribe(
        account = SoapSubscribeAccount.stripe(memberId,
          currency = supportedAccountCurrency(catalog)(joinData.zuoraAccountAddress.country, joinData.plan),
          autopay = true),
        paymentMethodOpt = Some(CreditCardReferenceTransaction(customer)),
        ratePlanId = ratePlanId,
        firstName = joinData.name.first,
        lastName = joinData.name.last,
        address = joinData.zuoraAccountAddress,
        casIdOpt = None,
        paymentDelay = None,
        ipAddressOpt = None,
        featureIds = featuresPerTier(zuoraFeatures)(joinData.plan, joinData.featureChoice).map(_.id)))
    } yield result

  def getPaymentSummary(memberId: ContactId): Future[PaymentSummary] = {
    for {
      subscription <- currentSubscription(memberId)
      invoiceItems <- zuoraSoapClient.query[SoapQueries.InvoiceItem](SimpleFilter("SubscriptionNumber", subscription.number))
    } yield {
      val filteredInvoices = latestInvoiceItems(invoiceItems)
      PaymentSummary(filteredInvoices, subscription.accountCurrency)
    }
  }

  def getMembershipSubscriptionSummary(contact: GenericSFContact): Future[ThankyouSummary] = {
    val latestSubF = currentSubscription(contact)
    def price(amount: Float)(implicit currency: Currency) = Price(amount, currency)
    def plan(sub: Subscription): (Price, BillingPeriod) = sub match {
      case p: PaidSubscription => (p.recurringPrice, p.plan.billingPeriod)
      case _ => (Price(0, sub.accountCurrency), Year)
    }

    def getSummaryViaInvoice =
      for {
        payment <- getPaymentSummary(contact)
        sub <- latestSubF
      } yield {
        implicit val currency = sub.accountCurrency
        val (planAmount, bp) = plan(sub)
        val nextPayment = Some(NextPayment(price(payment.current.price), payment.current.nextPaymentDate))

        ThankyouSummary(
          startDate = payment.current.serviceStartDate,
          amountPaidToday = price(payment.totalPrice),
          planAmount = planAmount,
          nextPayment = nextPayment,
          renewalDate = Some(payment.current.nextPaymentDate),
          initialFreePeriodOffer = false,
          billingPeriod = bp
        )
      }

    def getSummaryViaPreview =
      for {
        sub <- latestSubF
        paymentDetails <- paymentService.paymentDetails(contact, productFamily)
      } yield {
        implicit val currency = sub.accountCurrency
        val (planAmount, bp) = plan(sub)
        def price(amount: Float) = Price(amount, sub.accountCurrency)

        val nextPayment = for {
          pd <- paymentDetails
          amount <- pd.nextPaymentPrice
          date <- pd.nextPaymentDate
        } yield NextPayment(price(amount), date)

        ThankyouSummary(
          startDate = sub.startDate.toDateTimeAtCurrentTime(),
          amountPaidToday = price(0f),
          planAmount = planAmount,
          nextPayment = nextPayment,
          renewalDate = paymentDetails.map(_.termEndDate.plusDays(1)),
          sub.isInTrialPeriod,
          bp
        )
      }

    for {
      userInvoiced <- latestSubF.map(_.userHasBeenInvoiced)
      summary <- if (userInvoiced) getSummaryViaInvoice else getSummaryViaPreview
    } yield summary
  }

  def subscriptionUpgradableTo(memberId: SFMember, targetTier: PaidTier): Future[Option[model.Subscription]] = {
    import model.TierOrdering.upgradeOrdering

    membershipCatalog.get().zip(currentSubscription(memberId)).map { case (catalog, sub) =>
      val currentTier = memberId.tier
      val targetCurrencies = catalog.paidTierDetails(targetTier).currencies

      if (!sub.isInTrialPeriod && targetCurrencies.contains(sub.accountCurrency) && targetTier > currentTier) {
        Some(sub)
      } else None
    }
  }

  def cancelSubscription(contact: SFMember, user: IdMinimalUser): Future[String] = {
    val tp = TouchpointBackend.forUser(contact)

    for {
      sub <- subWithNoPendingAmend(contact)
      cancelDate = sub match {
        case p: PaidSubscription => p.chargedThroughDate.map(_.toDateTimeAtCurrentTime).getOrElse(DateTime.now)
        case _ => DateTime.now
      }
      _ <- zuoraSoapClient.authenticatedRequest(CancelPlan(sub.id, sub.ratePlanId, cancelDate))
    } yield {
      tp.memberRepository.metrics.putCancel(contact.tier)
      track(MemberActivity("cancelMembership", MemberData(contact.salesforceContactId, contact.identityId, contact.tier.name)), user)
      ""
    }
  }

  def downgradeSubscription(contact: SFMember, user: IdMinimalUser): Future[String] = {
    //if the member has paid upfront so they should have the higher tier until charged date has completed then be downgraded
    //otherwise use customer acceptance date (which should be in the future)
    def effectiveFrom(sub: model.PaidSubscription) = sub.chargedThroughDate.getOrElse(sub.firstPaymentDate).toDateTimeAtCurrentTime
    val tp = TouchpointBackend.forUser(contact)

    for {
      sub <- subWithNoPendingAmend(contact)
      paidSub = sub match {
        case p: PaidSubscription => p
        case _ => throw SubscriptionServiceError(s"Expected to downgrade a paid subscription")
      }
      friendRatePlanId <- findRatePlanId(FriendTierPlan.current)
      result <- zuoraSoapClient.authenticatedRequest(DowngradePlan(
        paidSub.id,
        paidSub.ratePlanId,
        friendRatePlanId,
        effectiveFrom(paidSub)))
    } yield {
      tp.memberRepository.metrics.putDowngrade(contact.tier)
      track(
        MemberActivity(
          "downgradeMembership",
          MemberData(
            contact.salesforceContactId,
            contact.identityId,
            contact.tier.name,
            Some(DowngradeAmendment(contact.tier)) //getting effective date and subscription annual / month is proving difficult
          )),
        user)

      ""
    }
  }

  def upgradeSubscription(contactId: ContactId, newTierPlan: TierPlan, preview: Boolean, featureChoice: Set[FeatureChoice]): Future[AmendResult] = {
    import SubscriptionService._
    for {
      sub <- subWithNoPendingAmend(contactId)
      zuoraFeatures <- zuoraSoapClient.featuresSupplier.get()
      newRatePlanId <- findRatePlanId(newTierPlan)
      choice = featuresPerTier(zuoraFeatures)(newTierPlan, featureChoice).map(_.id)
      result <- zuoraSoapClient.authenticatedRequest(
        UpgradePlan(sub.id, sub.ratePlanId, newRatePlanId, preview, choice))
    } yield result
  }

  private def findRatePlanId(newTierPlan: TierPlan): Future[String] = {
    membershipCatalog.get().map(_.ratePlanId(newTierPlan))
  }

  private def subWithNoPendingAmend(contactId: ContactId): Future[model.Subscription] =
    for {
      sub <- currentSubscription(contactId)
      status <- getSubscriptionStatus(sub.number)
    } yield {
      if (status.futureVersionIdOpt.isEmpty) {
       sub
      } else throw SubscriptionServiceError("Cannot amend subscription, amendments are already pending")
    }

  def getSubscriptionsByCasId(casId: String): Future[Seq[SoapQueries.Subscription]] =
    zuoraSoapClient.query[SoapQueries.Subscription](SimpleFilter("CASSubscriberID__c", casId))
}
