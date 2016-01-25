package services

import api.MemberService.{PendingAmendError, PaidSubscriptionExpected, MemberError}
import com.gu.i18n.Currency
import com.gu.identity.play.IdMinimalUser
import com.gu.membership.{FreeMembershipPlan, PaidMembershipPlan, MembershipPlan}
import com.gu.membership.util.Timing
import com.gu.memsub.BillingPeriod.year
import com.gu.memsub.Subscriber.{PaidMember, FreeMember}
import com.gu.memsub.Subscription.{MembershipSub, Paid, Plan, ProductRatePlanId}
import com.gu.memsub._
import com.gu.memsub.services.api.{CatalogService, PaymentService, SubscriptionService}
import com.gu.salesforce.Tier.{Partner, Patron}
import com.gu.salesforce._
import com.gu.stripe.Stripe.Customer
import com.gu.stripe.{Stripe, StripeService}
import com.gu.zuora.api.ZuoraService
import com.gu.zuora.api.ZuoraService.FeatureId
import com.gu.zuora.soap.actions.subscribe.{Account => SoapSubscribeAccount, CreditCardReferenceTransaction}
import com.gu.zuora.soap.models.Queries.PreviewInvoiceItem
import com.gu.zuora.soap.models.Results.{CreateResult, SubscribeResult, UpdateResult}
import com.gu.zuora.soap.models.{PaymentSummary, Queries => SoapQueries}
import controllers.IdentityRequest
import forms.MemberForm._
import model.Eventbrite.{EBCode, EBOrder, EBTicketClass}
import model.RichEvent.RichEvent
import model.{PaidSubscription, _}
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import tracking._
import utils.CampaignCode
import views.support.ThankyouSummary
import views.support.ThankyouSummary.NextPayment

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scalaz.{MonadTrans, EitherT, \/}
import scalaz.syntax.either._
import scalaz.std.scalaFuture._
import scalaz.syntax.monad._

object MemberService {
  import api.MemberService.MemberError

  type EitherTErr[F[_], A] = EitherT[F, MemberError, A]
  implicit val monadTrans = MonadTrans[EitherTErr]

  def featureIdsForTier(features: Seq[SoapQueries.Feature])(tier: Tier, choice: Set[FeatureChoice]): Seq[FeatureId] = {
    def chooseFeature(choices: Set[FeatureChoice]): Seq[FeatureId] =
      features.filter(f => choices.map(_.zuoraCode).contains(f.code))
        .map(_.id)

    tier match {
      case Patron() => chooseFeature(FeatureChoice.all)
      case Partner() => chooseFeature(choice).take(1)
      case _ => Nil
    }
  }
}

class MemberService(identityService: IdentityService,
                    salesforceService: api.SalesforceService,
                    zuoraService: ZuoraService,
                    stripeService: StripeService,
                    subscriptionService: SubscriptionService,
                    catalogService: CatalogService,
                    paymentService: PaymentService) extends api.MemberService with ActivityTracking {

  import EventbriteService._
  import MemberService._

  implicit val catalog = catalogService.membershipCatalog
  implicit val productFamily = Membership

  private val logger = Logger(getClass)

  override def createMember(user: IdMinimalUser,
                            formData: JoinForm,
                            identityRequest: IdentityRequest,
                            fromEventId: Option[String],
                            campaignCode: Option[CampaignCode]): Future[ContactId] = {

    val tier = formData.planChoice.tier

    val createContact: Future[ContactId] =
      for {
        user <- identityService.getFullUserDetails(user, identityRequest)
        contactId <- salesforceService.upsert(user, formData)
      } yield contactId

    Timing.record(salesforceService.metrics, "createMember") {
      formData.password.foreach(
        identityService.updateUserPassword(_, identityRequest, user.id))

      formData.password.foreach(identityService.updateUserPassword(_, identityRequest, user.id))

      val contactId = formData match {
        case paid: PaidMemberJoinForm =>
          for {
            customer <- stripeService.Customer.create(user.id, paid.payment.token)
            cId <- createContact
            subscription <- createPaidSubscription(cId, paid, customer, campaignCode)
            updatedMember <- salesforceService.updateMemberStatus(user, tier, Some(customer))
          } yield cId
        case _ =>
          for {
            cId <- createContact
            subscription <- createFreeSubscription(cId, formData)
            updatedMember <- salesforceService.updateMemberStatus(user, tier, None)
          } yield cId
      }

      contactId.map { cId =>
        identityService.updateUserFieldsBasedOnJoining(user, formData, identityRequest)

        salesforceService.metrics.putSignUp(tier)
        trackRegistration(formData, tier, cId, user, campaignCode)
        cId
      }
    }.andThen {
      case Success(contactId) =>
        logger.debug(s"createMember() success user=${user.id} memberAccount=$contactId")
        fromEventId.flatMap(EventbriteService.getBookableEvent).foreach { event =>
          event.service.wsMetrics.put(s"join-${tier.name}-event", 1)

          val memberData = MemberData(
            salesforceContactId = contactId.salesforceContactId,
            identityId = user.id,
            tier = tier,
            campaignCode = campaignCode)

          track(EventActivity("membershipRegistrationViaEvent", Some(memberData), EventData(event)), user)
        }
      case Failure(error: Stripe.Error) => logger.warn(s"Stripe API call returned error: '${error.getMessage()}' for user ${user.id}")
      case Failure(error) =>
        logger.error(s"Error in createMember() user=${user.id}", error)
        salesforceService.metrics.putFailSignUp(tier)
    }
  }

  override def upgradeFreeSubscription(subscriber: FreeMember,
                                       newTier: PaidTier,
                                       form: FreeMemberChangeForm,
                                       identityRequest: IdentityRequest,
                                       campaignCode: Option[CampaignCode]): Future[MemberError \/ ContactId] = {
    (for {
      customer <- stripeService.Customer.create(subscriber.contact.identityId, form.payment.token).liftM
      paymentResult <- createPaymentMethod(subscriber.contact, customer).liftM
      memberId <- EitherT(upgradeSubscription(
        subscriber.subscription,
        contact = subscriber.contact,
        planChoice = PaidPlanChoice(newTier, form.payment.billingPeriod),
        form = form,
        customerOpt = Some(customer),
        identityRequest = identityRequest,
        campaignCode = campaignCode
      ))
    } yield memberId).run
  }

  override def upgradePaidSubscription(subscriber: PaidMember,
                                       newTier: PaidTier,
                                       form: PaidMemberChangeForm,
                                       identityRequest: IdentityRequest,
                                       campaignCode: Option[CampaignCode]): Future[MemberError \/ ContactId] =
    (for {
      memberId <- EitherT(upgradeSubscription(
        subscriber.subscription,
        contact = subscriber.contact,
        planChoice = PaidPlanChoice(newTier, subscriber.subscription.plan.billingPeriod),
        form = form,
        customerOpt = None,
        identityRequest = identityRequest,
        campaignCode = campaignCode
      ))
    } yield memberId).run

  override def downgradeSubscription(subscriber: PaidMember): Future[MemberError \/ Unit] = {
    //if the member has paid upfront so they should have the higher tier until charged date has completed then be downgraded
    //otherwise use customer acceptance date (which should be in the future)
    def effectiveFrom(sub: model.PaidSubscription): DateTime = sub.chargedThroughDate.getOrElse(sub.firstPaymentDate).toDateTimeAtCurrentTime

    val friendRatePlanId = catalog.friend.productRatePlanId

    (for {
      paidSub <- EitherT(subOrPendingAmendError(subscriber.subscription))
      result <- zuoraService.downgradePlan(
        subscription = paidSub,
        futureRatePlanId = friendRatePlanId,
        effectiveFrom = effectiveFrom(paidSub)).liftM
    } yield {
      salesforceService.metrics.putDowngrade(subscriber.subscription.plan.tier)
      track(
        MemberActivity(
          "downgradeMembership",
          MemberData(
            salesforceContactId = subscriber.contact.salesforceContactId,
            identityId = subscriber.contact.identityId,
            tier = subscriber.subscription.plan.tier,
            tierAmendment = Some(DowngradeAmendment(subscriber.subscription.plan.tier)) //getting effective date and subscription annual / month is proving difficult
          )),
        subscriber.contact)
    }).run
  }

  override def cancelSubscription(subscriber: com.gu.memsub.Subscriber.Member): Future[MemberError \/ Unit] = {
    (for {
      sub <- EitherT(subOrPendingAmendError(subscriber.subscription))
      cancelDate = sub match {
        case p: Subscription with PaidPS[Plan] => p.chargedThroughDate.map(_.toDateTimeAtCurrentTime).getOrElse(DateTime.now)
        case _ => DateTime.now
      }
      _ <- zuoraService.cancelPlan(sub, cancelDate).liftM
    } yield {
      salesforceService.metrics.putCancel(subscriber.subscription.plan.tier)
      track(MemberActivity("cancelMembership", MemberData(
        subscriber.contact.salesforceContactId,
        subscriber.contact.identityId,
        subscriber.subscription.plan.tier)), subscriber.contact)
      track(MemberActivity("cancelMembership", MemberData(subscriber.contact.salesforceContactId, subscriber.contact.identityId, subscriber.subscription.plan.tier)), subscriber.contact)
    }).run
  }

  override def previewUpgradeSubscription(subscription: PaidSubscription,
                                          newRatePlanId: ProductRatePlanId): Future[Seq[PreviewInvoiceItem]] = {
    for {
      result <- zuoraService.upgradeSubscription(
        subscription = subscription,
        newRatePlanId = newRatePlanId,
        featureIds = Nil,
        preview = true)
    } yield result.invoiceItems
  }

  def subscriptionUpgradableTo(sub: Subscription with PaymentStatus[Plan], newTier: PaidTier): Boolean = {
    import model.TierOrdering.upgradeOrdering
    catalog.find(sub.productRatePlanId) exists { currentPlan =>
      val newPlan = catalog.findPaid(newTier)

      // The year and month plans are guaranteed to have the same currencies
      val targetCurrencies = newPlan.year.pricing.prices.map(_.currency).toSet
      !sub.isInTrialPeriod && targetCurrencies.contains(sub.currency) && newPlan.tier > currentPlan.tier
    }
  }

  override  def getMembershipSubscriptionSummary(contact: GenericSFContact): Future[ThankyouSummary] = {
    val latestSubF = subscriptionService.unsafeGet(contact)

    def price(amount: Float)(implicit currency: Currency) = Price(amount, currency)
    def plan(sub: Subscription): (Price, BillingPeriod) = sub match {
      case p: PaidSubscription => (p.recurringPrice, p.plan.billingPeriod)
      case _ => (Price(0, sub.currency), year)
    }

    def getSummaryViaInvoice =
      for {
        payment <- getPaymentSummary(contact)
        sub <- latestSubF
      } yield {
        implicit val currency = sub.currency
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
        paymentDetails <- paymentService.paymentDetails(contact)(Membership)
      } yield {
        implicit val currency = sub.currency
        val (planAmount, bp) = plan(sub)
        def price(amount: Float) = Price(amount, sub.currency)

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

  override def getUsageCountWithinTerm(subscription: Subscription, unitOfMeasure: String): Future[Option[Int]] = {
    val features = subscription.features
    val startDate = subscription.startDate.toDateTimeAtCurrentTime()
    zuoraService.getUsages(subscription.name, unitOfMeasure, startDate).map { usages =>
      val hasComplimentaryTickets = features.map(_.get).contains(FreeEventTickets.zuoraCode)
      if (!hasComplimentaryTickets) None else Some(usages.size)
    }
  }

  override def recordFreeEventUsage(subs: Subscription, event: RichEvent, order: EBOrder, quantity: Int): Future[CreateResult] = {
    val description = s"event-id:${event.id};order-id:${order.id}"

    for {
      result <- zuoraService.createFreeEventUsage(
        accountId = subs.accountId,
        subscriptionNumber = subs.name,
        description = description,
        quantity = quantity
      )
    } yield {
      logger.info(s"Recorded a complimentary event ticket usage for account ${subs.accountId}, subscription: ${subs.name}, details: $description")
      result
    }
  }

  override def retrieveComplimentaryTickets(sub: Subscription, event: RichEvent): Future[Seq[EBTicketClass]] = {
    Timing.record(salesforceService.metrics, "retrieveComplimentaryTickets") {
      for {
        usageCount <- getUsageCountWithinTerm(sub, FreeEventTickets.unitOfMeasure)
      } yield {
        val hasComplimentaryTickets = usageCount.isDefined
        val allowanceNotExceeded = usageCount.exists(_ < FreeEventTickets.allowance)
        logger.info(
          s"User ${sub.accountId} has used $usageCount tickets" ++
            s"(allowance not exceeded: $allowanceNotExceeded, is entitled: $hasComplimentaryTickets)")

        if (hasComplimentaryTickets && allowanceNotExceeded)
          event.internalTicketing.map(_.complimentaryTickets).getOrElse(Nil)
        else Nil
      }
    }
  }

  override def createEBCode(subscriber: com.gu.memsub.Subscriber.Member, event: RichEvent): Future[Option[EBCode]] = {
    retrieveComplimentaryTickets(subscriber.subscription, event).flatMap { complimentaryTickets =>
      val code = DiscountCode.generate(s"A_${subscriber.contact.identityId}_${event.id}")
      val unlockedTickets = complimentaryTickets ++ event.retrieveDiscountedTickets(subscriber.subscription.plan.tier)
      event.service.createOrGetAccessCode(event, code, unlockedTickets)
    }
  }

  override def createFreeSubscription(contactId: ContactId,
                                      joinData: JoinForm): Future[SubscribeResult] = {
    val planId = joinData.planChoice.productRatePlanId
    val currency = catalog.unsafeFindFree(planId).currencyOrGBP(joinData.deliveryAddress.country)

    for {
      zuoraFeatures <- zuoraService.getFeatures
      result <- zuoraService.createSubscription(
        subscribeAccount = SoapSubscribeAccount.stripe(contactId, currency, autopay = false),
        paymentMethod = None,
        productRatePlanId = planId,
        name = joinData.name,
        address = joinData.deliveryAddress
      )
    } yield result
  }

  implicit private def features = zuoraService.getFeatures

  override def createPaidSubscription(contactId: ContactId,
                                      joinData: PaidMemberJoinForm,
                                      customer: Stripe.Customer,
                                      campaignCode: Option[CampaignCode]): Future[SubscribeResult] =
    for {
      zuoraFeatures <- zuoraService.getFeatures
      planId = joinData.planChoice.productRatePlanId
      currency = catalog.unsafeFindPaid(planId).currencyOrGBP(joinData.zuoraAccountAddress.country)
      result <- zuoraService.createSubscription(
        subscribeAccount = SoapSubscribeAccount.stripe(
          contactId = contactId,
          currency = currency,
          autopay = true),
        paymentMethod = Some(CreditCardReferenceTransaction(customer)),
        productRatePlanId = planId,
        name = joinData.name,
        address = joinData.zuoraAccountAddress,
        featureIds = featuresPerTier(zuoraFeatures)(planId, joinData.featureChoice).map(_.id)
      )
    } yield result

  private def featuresPerTier(zuoraFeatures: Seq[SoapQueries.Feature])(productRatePlanId: ProductRatePlanId, choice: Set[FeatureChoice]): Seq[SoapQueries.Feature] = {
    def byChoice(choice: Set[FeatureChoice]) =
      zuoraFeatures.filter(f => choice.map(_.zuoraCode).contains(f.code))

    catalog.unsafeFind(productRatePlanId).tier match {
      case Patron() => byChoice(FeatureChoice.all)
      case Partner() => byChoice(choice).take(1)
      case _ => Nil
    }
  }

  def latestInvoiceItems(items: Seq[SoapQueries.InvoiceItem]): Seq[SoapQueries.InvoiceItem] = {
    if(items.isEmpty)
      items
    else {
      val sortedItems = items.sortBy(_.chargeNumber)
      sortedItems.filter(_.subscriptionId == sortedItems.last.subscriptionId)
    }
  }

  private def upgradeSubscription(subscription: MembershipSub,
                                  contact: Contact,
                                  planChoice: PlanChoice,
                                  form: MemberChangeForm,
                                  customerOpt: Option[Customer],
                                  identityRequest: IdentityRequest,
                                  campaignCode: Option[CampaignCode]): Future[MemberError \/ ContactId] = {

    val addressDetails = form.addressDetails
    val newPlan = catalog.unsafeFindPaid(planChoice.productRatePlanId)
    val tier = newPlan.tier

    addressDetails.foreach(
      identityService.updateUserFieldsBasedOnUpgrade(contact.identityId, _, identityRequest))

    (for {
      _ <- salesforceService.updateMemberStatus(IdMinimalUser(contact.identityId, None), tier, customerOpt).liftM
      sub <- EitherT(subOrPendingAmendError(subscription))
      featureIds <- zuoraService.getFeatures.map { fs =>
        featureIdsForTier(fs)(tier, form.featureChoice)
      }.liftM
      _ <- zuoraService.upgradeSubscription(sub, newPlan.productRatePlanId, featureIds, preview = false).liftM
    } yield {
      salesforceService.metrics.putUpgrade(tier)
      trackUpgrade(contact, sub, newPlan, addressDetails, campaignCode)
      contact
    }).run
  }

  private def createPaymentMethod(contactId: ContactId,
                                  customer: Stripe.Customer): Future[UpdateResult] =
    for {
      sub <- subscriptionService.unsafeGet(contactId)
      result <- zuoraService.createCreditCardPaymentMethod(sub.accountId, customer)
    } yield result

  private def subOrPendingAmendError[P <: Subscription](sub: P): Future[MemberError \/ P] =
    for {
      status <- zuoraService.getSubscriptionStatus(sub.name)
    } yield {
      if (status.futureVersionOpt.isDefined)
        PendingAmendError(sub.name).left
      else
        sub.right
    }

  private def getPaymentSummary(memberId: ContactId): Future[PaymentSummary] =
    subscriptionService.unsafeGet(memberId).flatMap { sub =>
      zuoraService.getPaymentSummary(sub.name, sub.currency)
    }
}
