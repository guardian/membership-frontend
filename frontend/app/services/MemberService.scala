package services

import com.gu.config.DiscountRatePlanIds
import com.gu.i18n.{CountryGroup, Currency}
import com.gu.identity.play.IdMinimalUser
import com.gu.membership.MembershipCatalog
import com.gu.memsub.BillingPeriod.year
import com.gu.memsub.Subscriber.{FreeMember, PaidMember}
import com.gu.memsub.Subscription.{Feature, MembershipSub, Plan, ProductRatePlanId}
import com.gu.memsub.promo.PromoCode
import com.gu.memsub.services.PromoService
import com.gu.memsub.util.Timing
import com.gu.services.model.BillingSchedule
import com.gu.subscriptions.Discounter
import com.gu.zuora.rest
import com.gu.zuora.soap.models.Commands._
import com.gu.zuora.soap.models.Commands.Lenses._
import services.api.MemberService.{MemberError, PendingAmendError}
import com.gu.memsub._
import com.gu.memsub.services.api.{CatalogService, PaymentService, SubscriptionService}
import com.gu.salesforce.Tier.{Partner, Patron}
import com.gu.salesforce._
import com.gu.stripe.Stripe.Customer
import com.gu.stripe.{Stripe, StripeService}
import com.gu.zuora.api.ZuoraService
import com.gu.zuora.soap.models.Results.{CreateResult, SubscribeResult, UpdateResult}
import com.gu.zuora.soap.models.{PaymentSummary, Queries => SoapQueries}
import controllers.IdentityRequest
import forms.MemberForm._
import model.Eventbrite.{EBCode, EBOrder, EBTicketClass}
import model.RichEvent.RichEvent
import model.{PaidSubscription, _}
import org.joda.time.{DateTimeZone, DateTime}
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import tracking._
import utils.CampaignCode
import views.support.ThankyouSummary
import views.support.ThankyouSummary.NextPayment

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scalaz.std.scalaFuture._
import scalaz.syntax.either._
import scalaz.syntax.std.option._
import scalaz.syntax.monad._
import scalaz._

object MemberService {
  import api.MemberService.MemberError

  type EitherTErr[F[_], A] = EitherT[F, MemberError, A]
  implicit val monadTrans = MonadTrans[EitherTErr]

  def featureIdsForTier(features: Seq[SoapQueries.Feature])(tier: Tier, choice: Set[FeatureChoice]): Seq[Feature.Id] = {
    def chooseFeature(choices: Set[FeatureChoice]): Seq[Feature.Id] =
      features.filter(f => choices.map(_.zuoraCode).contains(f.code))
        .map(_.id)

    tier match {
      case Patron() => chooseFeature(FeatureChoice.all)
      case Partner() => chooseFeature(choice).take(1)
      case _ => Nil
    }
  }

  def getRatePlanIdsToRemove(current: Seq[rest.RatePlan],
                             planFinder: ProductRatePlanId => Option[Plan],
                             discounts: DiscountRatePlanIds) = current.collect {
    case discount if discount.productRatePlanId == discounts.percentageDiscount.planId.get => discount.id
    case plan if planFinder(ProductRatePlanId(plan.productRatePlanId)).isDefined => plan.id
  }
}

class MemberService(identityService: IdentityService,
                    salesforceService: api.SalesforceService,
                    zuoraService: ZuoraService,
                    stripeService: StripeService,
                    subscriptionService: SubscriptionService[MembershipCatalog],
                    catalogService: CatalogService,
                    promoService: PromoService,
                    paymentService: PaymentService,
                    discounter: Discounter,
                    discountIds: DiscountRatePlanIds) extends api.MemberService with ActivityTracking {

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
        user <- identityService.getFullUserDetails(user)(identityRequest)
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
            subscription <- createPaidSubscription(cId, paid, paid.name, paid.tier, customer, campaignCode)
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

      case Failure(error) => salesforceService.metrics.putFailSignUp(tier)
    }
  }

  override def upgradeFreeSubscription(friend: FreeMember, newTier: PaidTier, form: FreeMemberChangeForm, code: Option[CampaignCode])
                                      (implicit identity: IdentityRequest): Future[MemberError \/ ContactId] = {
    (for {
      sub <- EitherT(subOrPendingAmendError(friend.subscription))
      _ <- zuoraService.cancelPlan(sub, DateTime.now.toLocalDate).liftM
      customer <- stripeService.Customer.create(friend.contact.identityId, form.payment.token).liftM
      paymentResult <- createPaymentMethod(friend.contact, customer).liftM
      subRes <- createPaidSubscription(friend.contact,form,NameForm(friend.contact.firstName.getOrElse(""),friend.contact.lastName),newTier,customer,code).liftM
    } yield {
      track(MemberActivity("upgradeMembership", MemberData(
        friend.contact.salesforceContactId,
        friend.contact.identityId,
        newTier)), friend.contact)
      salesforceService.metrics.putUpgrade(newTier)
      friend.contact}).run

  }

  override def upgradePaidSubscription(sub: PaidMember, newTier: PaidTier, form: PaidMemberChangeForm, code: Option[CampaignCode])
                                      (implicit id: IdentityRequest): Future[MemberError \/ ContactId] =
    (for {
      memberId <- EitherT(upgradeSubscription(
        sub.subscription,
        contact = sub.contact,
        planChoice = PaidPlanChoice(newTier, sub.subscription.plan.billingPeriod),
        form = form,
        customerOpt = None,
        campaignCode = code
      ))
    } yield {
      track(MemberActivity("upgradeMembership", MemberData(
        sub.contact.salesforceContactId,
        sub.contact.identityId,
        sub.subscription.plan.tier)), sub.contact)
      salesforceService.metrics.putUpgrade(newTier)
      memberId}).run

  override def downgradeSubscription(subscriber: PaidMember): Future[MemberError \/ Unit] = {
    //if the member has paid upfront so they should have the higher tier until charged date has completed then be downgraded
    //otherwise use customer acceptance date (which should be in the future)
    def effectiveFrom(sub: model.PaidSubscription) = sub.chargedThroughDate.getOrElse(sub.firstPaymentDate)

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
          "upgradeMembership",
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
        case p: Subscription with PaidPS[Plan] => p.chargedThroughDate.getOrElse(DateTime.now.toLocalDate)
        case _ => DateTime.now.toLocalDate
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

  override def previewUpgradeSubscription(subscriber: PaidMember, newPlan: PlanChoice, code: Option[PromoCode])
                                         (implicit i: IdentityRequest): Future[MemberError \/ BillingSchedule] = {
    (for {
      _ <- EitherT(subOrPendingAmendError(subscriber.subscription))
      a <- EitherT(amend(subscriber.subscription, subscriber.contact, newPlan, Set.empty, code).map(\/.right))
      result <- EitherT(zuoraService.upgradeSubscription(a.copy(previewMode = true)).map(\/.right))
    } yield BillingSchedule.fromPreviewInvoiceItems(result.invoiceItems).getOrElse(
      throw new IllegalStateException(s"Sub ${subscriber.subscription.id} upgrading to ${newPlan.tier} has no bills")
    )).run
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
    val latestSubEither = subscriptionService.getEither(contact).map(_.get)
    val latestSubF = latestSubEither.map(_.fold(identity,identity))

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
        subEither <- latestSubEither
        paymentDetails <- paymentService.paymentDetails(subEither)
      } yield {
        implicit val currency = sub.currency
        val (planAmount, bp) = plan(sub)
        def price(amount: Float) = Price(amount, sub.currency)

        val nextPayment = for {
          amount <- paymentDetails.nextPaymentPrice
          date <- paymentDetails.nextPaymentDate
        } yield NextPayment(price(amount), date)

        ThankyouSummary(
          startDate = sub.startDate,
          amountPaidToday = price(0f),
          planAmount = planAmount,
          nextPayment = nextPayment,
          renewalDate = Some(paymentDetails.termEndDate.plusDays(1)),
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
    val startDate = subscription.startDate.toDateTimeAtStartOfDay(DateTimeZone.forID("America/Los_Angeles"))
    zuoraService.getUsages(subscription.name, unitOfMeasure, startDate).map { usages =>
      val hasComplimentaryTickets = features.map(_.code).contains(FreeEventTickets.zuoraCode)
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

        logger.info(s"Complementary tickets: ${event.internalTicketing.map(_.complimentaryTickets)}")
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
      result <- zuoraService.createSubscription(Subscribe(
        account = Account.stripe(contactId, currency, autopay = false),
        paymentMethod = None,
        ratePlans = NonEmptyList(RatePlan(planId.get, None)),
        name = joinData.name,
        address = joinData.deliveryAddress
      ))
    } yield result
  }

  implicit private def features = zuoraService.getFeatures

  override def createPaidSubscription(contactId: ContactId,
                                      joinData: PaidMemberForm,
                                      nameData: NameForm,
                                      tier: PaidTier,
                                      customer: Stripe.Customer,
                                      campaignCode: Option[CampaignCode]): Future[SubscribeResult] = {

    val subscribe = zuoraService.getFeatures map { features =>

      val planChoice = PaidPlanChoice(tier,joinData.payment.billingPeriod)
      val planId = planChoice.productRatePlanId
      val plan = RatePlan(planId.get, None, featuresPerTier(features)(planId, joinData.featureChoice).map(_.id.get))
      val currency = catalog.unsafeFindPaid(planId).currencyOrGBP(joinData.zuoraAccountAddress.country)

      Subscribe(account = Account.stripe(contactId = contactId, currency = currency, autopay = true),
              paymentMethod = CreditCardReferenceTransaction(customer.card.id, customer.id).some,
              address = joinData.zuoraAccountAddress,
              ratePlans = NonEmptyList(plan),
              name = nameData)
    }

    subscribe.map(promoService.applyPromotion(_, joinData.promoCode, joinData.zuoraAccountAddress.country.some))
             .flatMap(zuoraService.createSubscription)
  }

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

  /**
    * Construct an Amend command
    */
  private def amend(sub: MembershipSub, contact: Contact, planChoice: PlanChoice, form: Set[FeatureChoice], code: Option[PromoCode])
                   (implicit r: IdentityRequest): Future[Amend] = {

    val newPlan = catalog.unsafeFindPaid(planChoice.productRatePlanId)
    val tier = newPlan.tier

    val oldRest = zuoraService.getRestSubscription(sub.name).map(_.getOrElse(
      throw new Exception(s"REST sub not found for ${sub.id}")
    ))

    val zuoraFeatures = zuoraService.getFeatures.map { fs => featureIdsForTier(fs)(tier, form) }
    val newRatePlan = zuoraFeatures.map(fs => RatePlan(newPlan.productRatePlanId.get, None, fs.map(_.get)))

    val country = identityService.getFullUserDetails(IdMinimalUser(contact.identityId, None))
      .map(_.privateFields.flatMap(_.country).flatMap(CountryGroup.countryByNameOrCode))

    (country |@| newRatePlan |@| oldRest) { case (ctry, newPln, restSub) =>
      val plansToRemove = getRatePlanIdsToRemove(restSub.ratePlans, catalog.find, discountIds)
      val upgrade = Amend(sub.id.get, plansToRemove, NonEmptyList(newPln), sub.promoCode)
      promoService.applyPromotion(upgrade, code, ctry)
    }
  }

  private def upgradeSubscription(sub: MembershipSub,
                                  contact: Contact,
                                  planChoice: PlanChoice,
                                  form: MemberChangeForm,
                                  customerOpt: Option[Customer],
                                  campaignCode: Option[CampaignCode])(implicit r: IdentityRequest): Future[MemberError \/ ContactId] = {

    val addressDetails = form.addressDetails
    val newPlan = catalog.unsafeFindPaid(planChoice.productRatePlanId)
    val tier = newPlan.tier

    (for {
      s <- EitherT(subOrPendingAmendError(sub))
      command <- EitherT(amend(sub, contact, planChoice, form.featureChoice, form.promoCode).map(\/.right))
      _ <- salesforceService.updateMemberStatus(IdMinimalUser(contact.identityId, None), newPlan.tier, customerOpt).liftM
      _ <- zuoraService.upgradeSubscription(command).liftM
    } yield {
      salesforceService.metrics.putUpgrade(tier)
      addressDetails.foreach(identityService.updateUserFieldsBasedOnUpgrade(contact.identityId, _))
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
      restSub <- zuoraService.getRestSubscription(sub.name).map(
        _.getOrElse(throw new Exception(s"Sub ${sub.name} not found with rest"))
      )
    } yield {
      if (restSub.hasPendingAmendment() || restSub.isCancelled)
        PendingAmendError(sub.name).left
      else
        sub.right
    }

  private def getPaymentSummary(memberId: ContactId): Future[PaymentSummary] =
    subscriptionService.unsafeGet(memberId).flatMap { sub =>
      zuoraService.getPaymentSummary(sub.name, sub.currency)
    }
}
