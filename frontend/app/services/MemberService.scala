package services

import _root_.services.api.MemberService.{MemberError, PendingAmendError}
import _root_.services.paymentmethods._
import com.gu.config.DiscountRatePlanIds
import com.gu.i18n.Country.{Australia, UK}
import com.gu.i18n.Currency.GBP
import com.gu.i18n.{Country, CountryGroup, Currency}
import com.gu.identity.play.{IdMinimalUser, IdUser}
import com.gu.memsub.Subscriber.{FreeMember, PaidMember}
import com.gu.memsub.Subscription.{Feature, ProductRatePlanId, RatePlanId}
import com.gu.memsub.services.api.PaymentService
import com.gu.memsub.subsv2.reads.ChargeListReads._
import com.gu.memsub.subsv2.reads.SubPlanReads._
import com.gu.memsub.subsv2.services.{SubIds, _}
import com.gu.memsub.subsv2.{SubscriptionPlan, _}
import com.gu.memsub.util.Timing
import com.gu.memsub.{BillingPeriod, BillingSchedule, Price}
import com.gu.salesforce.Tier.{Partner, Patron}
import com.gu.salesforce._
import com.gu.stripe.Stripe.Customer
import com.gu.stripe.StripeService
import com.gu.subscriptions.Discounter
import com.gu.zuora.ZuoraRestService
import com.gu.zuora.api._
import com.gu.zuora.soap.models.Commands.{PaymentMethod, _}
import com.gu.zuora.soap.models.Results.{CreateResult, SubscribeResult, UpdateResult}
import com.gu.zuora.soap.models.errors.PaymentGatewayError
import com.gu.zuora.soap.models.{Queries => SoapQueries}
import com.typesafe.scalalogging.LazyLogging
import controllers.IdentityRequest
import forms.MemberForm._
import model.Eventbrite.{EBCode, EBOrder, EBTicketClass}
import model.RichEvent.RichEvent
import model.{Benefit => _, _}
import org.joda.time.{DateTime, DateTimeZone}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import tracking._
import utils.ReferralData
import views.support.MembershipCompat._
import views.support.ThankyouSummary
import views.support.ThankyouSummary.NextPayment

import scala.concurrent.Future
import scala.util.{Failure, Success}
import scalaz._
import scalaz.std.scalaFuture._
import scalaz.syntax.either._
import scalaz.syntax.monad._

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

  def getDiscountRatePlanIdsToRemove(current: Seq[SubIds], discounts: DiscountRatePlanIds): Seq[RatePlanId] = current.collect {
    case discount if discount.productRatePlanId == discounts.percentageDiscount.planId => discount.ratePlanId
  }
}

class MemberService(identityService: IdentityService,
                    salesforceService: api.SalesforceService,
                    zuoraService: ZuoraService,
                    zuoraRestService: ZuoraRestService[Future],
                    ukStripeService: StripeService,
                    auStripeService: StripeService,
                    payPalService: PayPalService,
                    subscriptionService: SubscriptionService[Future],
                    catalogService: CatalogService[Future],
                    paymentService: PaymentService,
                    discounter: Discounter,
                    discountIds: DiscountRatePlanIds)
  extends api.MemberService with ActivityTracking with LazyLogging {

  import EventbriteService._
  import MemberService._

  val availablePaymentMethods = new AvailablePaymentMethods(Set(
    new StripeInitialiser(ukStripeService, countryBlacklist = Set(Australia)),
    new StripeInitialiser(auStripeService, countryWhitelist = Set(Australia)),
    new PayPalInitialiser(payPalService)
  ))

  implicit val catalog = catalogService.unsafeCatalog

  def country(contact: Contact)(implicit r: IdentityRequest): Future[Country] =
    identityService.getFullUserDetails(IdMinimalUser(contact.identityId, None))
      .map(c => c.privateFields.flatMap(_.billingCountry).orElse(c.privateFields.flatMap(_.country))
        .flatMap(CountryGroup.countryByNameOrCode)).map(_.getOrElse(Country.UK))

  override def createMember(
      user: IdUser,
      formData: JoinForm,
      fromEventId: Option[String],
      tier: Tier,
      ipCountry: Option[Country],
      referralData: ReferralData): Future[(ContactId, ZuoraSubName)] = {

    def createFreeZuoraSubscription(sfContact: ContactId, formData: JoinForm, email: String) =
      (for {
        zuoraSub <- createFreeSubscription(sfContact, user.id, formData, email, ipCountry)
      } yield zuoraSub.subscriptionName).andThen { case Failure(e) =>
        logger.error(s"Could not create free Zuora subscription for user ${user.id}", e)
      }

    def createPaidZuoraSubscription(sfContact: ContactId, paid: PaidMemberJoinForm, email: String, paymentMethod: PaymentMethod): Future[String] =
      (for {
        zuoraSub <- createPaidSubscription(sfContact, user.id, paid, paid.name, paid.tier, email, paymentMethod, ipCountry)
      } yield zuoraSub.subscriptionName).andThen {
        case Failure(e: PaymentGatewayError) => logger.warn(s"Could not create paid Zuora membership due to payment gateway failure: ID=${user.id}, Country: ${paid.billingAddress.flatMap(_.country)}", e)
        case Failure(e) => logger.error(s"Could not create paid Zuora subscription: ID=${user.id}", e)
      }

    def updateSalesforceContactWithMembership(stripeCustomer: Option[Customer]): Future[ContactId] =
      salesforceService.updateMemberStatus(user.minimal, tier, stripeCustomer).andThen { case Failure(e) =>
        logger.error(s"Could not update Salesforce contact with membership status for user ${user.id}", e)
      }

    formData match {
      case payingForm: PaidMemberJoinForm =>
        val transactingCountry = payingForm.billingAddress.flatMap(_.country) orElse payingForm.deliveryAddress.country orElse ipCountry getOrElse UK
        for {
          paymentMethod <- availablePaymentMethods.deriveInitialiserAndTokenFrom(payingForm.payment, transactingCountry).initialiseUsing(user.minimal)
          sfContact <- createSalesforceContact(user, formData)
          zuoraSubName <- createPaidZuoraSubscription(sfContact, payingForm, user.primaryEmailAddress, paymentMethod)
          _ <- updateSalesforceContactWithMembership(None) // FIXME: This should go!
        } yield (sfContact, zuoraSubName)
      case _ =>
        for {
          sfContact <- createSalesforceContact(user, formData)
          zuoraSubName <- createFreeZuoraSubscription(sfContact, formData, user.primaryEmailAddress)
          _ <- updateSalesforceContactWithMembership(None) // FIXME: This should go!
        } yield (sfContact, zuoraSubName)
    }
  }

  def createSalesforceContact(user: IdUser, formData: CommonForm): Future[ContactId] =
    salesforceService.upsert(user, formData).andThen { case Failure(e) =>
      logger.error(s"Could not create Salesforce contact for user ${user.id}", e)
    }

  def retrieveEmail(baid: String) = Future {
    payPalService.retrieveEmail(baid)
  }

  override def upgradeFreeSubscription(sub: FreeMember, newTier: PaidTier, form: FreeMemberChangeForm, referralData: ReferralData)
                                      (implicit identity: IdentityRequest): Future[MemberError \/ ContactId] = {
    (for {
      _ <- createPaymentMethod(sub, form).liftM
      memberId <- EitherT(upgradeSubscription(
        sub.subscription,
        contact = sub.contact,
        planChoice = PaidPlanChoice(newTier, form.payment.billingPeriod),
        form = form,
        referralData = referralData
      ))
    } yield memberId).run
  }

  override def upgradePaidSubscription(sub: PaidMember, newTier: PaidTier, form: PaidMemberChangeForm, referralData: ReferralData)
                                      (implicit id: IdentityRequest): Future[MemberError \/ ContactId] =
    (for {
      memberId <- EitherT(upgradeSubscription(
        sub.subscription,
        contact = sub.contact,
        planChoice = PaidPlanChoice(newTier, sub.subscription.plan.charges.billingPeriod),
        form = form,
        referralData = referralData
      ))
    } yield {
      track(MemberActivity("upgradeMembership", MemberData(
        sub.contact.salesforceContactId,
        sub.contact.identityId,
        sub.subscription.plan.tier)), sub.contact)
      salesforceService.metrics.putUpgrade(newTier)
      memberId
    }).run

  override def downgradeSubscription(subscriber: PaidMember): Future[MemberError \/ Unit] = {
    //if the member has paid upfront so they should have the higher tier until charged date has completed then be downgraded
    //otherwise use customer acceptance date (which should be in the future)
    def effectiveFrom(sub: Subscription[SubscriptionPlan.PaidMember]) = sub.plan.chargedThrough.getOrElse(sub.startDate)

    val friendRatePlanId = catalog.friend.id

    (for {
      paidSub <- EitherT(Future.successful(subOrPendingAmendError(subscriber.subscription)))
      result <- zuoraService.downgradePlan(
        subscription = paidSub.id,
        currentRatePlanId = paidSub.plan.id,
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

    val cancelDate = subscriber.subscription.plan match {
      case PaidSubscriptionPlan(_, _, _, _, _, _, _, _, chargedThrough, _, _) => chargedThrough.getOrElse(DateTime.now.toLocalDate)
      case _ => DateTime.now.toLocalDate
    }

    (for {
      _ <- zuoraService.cancelPlan(subscriber.subscription.id, subscriber.subscription.plan.id, cancelDate).liftM
    } yield {
      salesforceService.metrics.putCancel(subscriber.subscription.plan.tier)
      track(MemberActivity("cancelMembership", MemberData(
        subscriber.contact.salesforceContactId,
        subscriber.contact.identityId,
        subscriber.subscription.plan.tier)), subscriber.contact)
      track(MemberActivity("cancelMembership", MemberData(subscriber.contact.salesforceContactId, subscriber.contact.identityId, subscriber.subscription.plan.tier)), subscriber.contact)
    }).run
  }

  override def previewUpgradeSubscription(subscriber: PaidMember, newPlan: PlanChoice)
                                         (implicit i: IdentityRequest): Future[MemberError \/ BillingSchedule] = {
    (for {
      _ <- EitherT(Future.successful(subOrPendingAmendError(subscriber.subscription)))
      country <- EitherT(country(subscriber.contact).map(\/.right))
      a <- EitherT(amend(subscriber.subscription, newPlan, Set.empty).map(\/.right))
      result <- EitherT(zuoraService.upgradeSubscription(a.copy(previewMode = true)).map(\/.right))
    } yield BillingSchedule.fromPreviewInvoiceItems(_ => None)(result.invoiceItems).getOrElse(
      throw new IllegalStateException(s"Sub ${subscriber.subscription.id} upgrading to ${newPlan.tier} has no bills")
    )).run
  }

  def upgradeTierIsValidForCurrency(sub: Subscription[SubscriptionPlan.Member], newTier: PaidTier): Boolean = {
    val newPlan = catalog.findPaid(newTier)
    // The year and month plans are guaranteed to have the same currencies
    val targetCurrencies = newPlan.year.charges.price.prices.map(_.currency).toSet
    val currencyIsAvailable = targetCurrencies.contains(sub.plan.currency)
    currencyIsAvailable
  }

  def upgradeTierIsHigher(sub: Subscription[SubscriptionPlan.Member], newTier: PaidTier): Boolean = {
    import model.TierOrdering.upgradeOrdering
    val newPlan = catalog.findPaid(newTier)
    val higherTier = newPlan.month.tier > sub.plan.tier
    higherTier
  }

  override def getMembershipSubscriptionSummary(contact: GenericSFContact): Future[ThankyouSummary] = {

    val latestSubResponse = subscriptionService.either[SubscriptionPlan.FreeMember, SubscriptionPlan.PaidMember](contact)
    val latestSubEither = latestSubResponse.map(_.toOption.flatten.get)
    val latestSubF = latestSubEither.map(_.fold(identity, identity))

    for {
      sub <- latestSubF
      subEither <- latestSubEither
      upcomingPaymentDetails <- paymentService.billingSchedule(sub.id) // shows things you'll pay assuming infinite term
      paymentToday <- zuoraService.getPaymentSummary(sub.name, sub.plan.charges.currencies.head).map(Some.apply).recover({ case _ => None })
    } yield {
      implicit val currency = sub.plan.charges.currencies.head

      def price(amount: Float) = Price(amount, sub.plan.charges.currencies.head)

      val nextPayment = for {
        amount <- upcomingPaymentDetails.map(_.first.amount)
        date <- upcomingPaymentDetails.map(_.first.date)
      } yield NextPayment(price(amount), date)

      val planAmount = subEither.fold(_ => Price(0.0f, GBP), _.plan.charges.price.prices.head)

      ThankyouSummary(
        startDate = sub.termStartDate,
        amountPaidToday = Price(paymentToday.map(_.totalPrice).getOrElse(0f), planAmount.currency),
        planAmount = planAmount,
        nextPayment = nextPayment,
        renewalDate = Some(sub.termEndDate.plusDays(1)),
        initialFreePeriodOffer = subEither.fold(_ => false, _.plan.chargedThrough.isEmpty),
        subEither.fold(_ => BillingPeriod.Year, _.plan.charges.billingPeriod) // TODO should be optional for free plans?
      )
    }

  }

  override def getUsageCountWithinTerm(subscription: Subscription[SubscriptionPlan.Member], unitOfMeasure: String): Future[Option[Int]] = {
    val features = subscription.plan match {
      case plan: PaidSubscriptionPlan[_, _] => plan.features
      case _ => List.empty
    }
    val startDate = subscription.termStartDate.toDateTimeAtStartOfDay(DateTimeZone.forID("America/Los_Angeles"))

    zuoraService.getUsages(subscription.name, unitOfMeasure, startDate).map { usages =>
      logger.info(s"getUsageCountWithinTerm: User ${subscription.accountId} has used ${usages.size} tickets since $startDate " +
        s"(sub-start-date=${subscription.startDate} term-start-date=${subscription.termStartDate})")

      val hasComplimentaryTickets = features.map(_.code).contains(FreeEventTickets.zuoraCode)
      if (!hasComplimentaryTickets) None else Some(usages.size)
    }
  }

  override def recordFreeEventUsage(subs: Subscription[SubscriptionPlan.Member], event: RichEvent, order: EBOrder, quantity: Int): Future[CreateResult] = {
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

  override def retrieveComplimentaryTickets(sub: Subscription[SubscriptionPlan.Member], event: RichEvent): Future[Seq[EBTicketClass]] = {
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
                                      identityId: String,
                                      joinData: JoinForm,
                                      email: String,
                                      ipCountry: Option[Country]): Future[SubscribeResult] = {
    val planId = joinData.planChoice.productRatePlanId
    val today = DateTime.now.toLocalDate
    val transactingCountry = joinData.deliveryAddress.country orElse ipCountry getOrElse UK
    val currency = catalog.unsafeFindFree(planId).currencyOrGBP(joinData.deliveryAddress.country.getOrElse(UK))
    val paymentGateway = if (transactingCountry == Australia) auStripeService.paymentGateway else ukStripeService.paymentGateway

    (for {
      zuoraFeatures <- zuoraService.getFeatures
      result <- zuoraService.createSubscription(Subscribe(
        account = Account(contactId, identityId, currency, autopay = false, paymentGateway),
        paymentMethod = None,
        ratePlans = NonEmptyList(RatePlan(planId.get, None)),
        name = joinData.name,
        address = joinData.deliveryAddress,
        email = email,
        promoCode = None,
        contractAcceptance = today,
        contractEffective = today
      ))
    } yield result).andThen { case Failure(e) => logger.error(s"Could not create free subscription for user with salesforceContactId ${contactId.salesforceContactId}", e) }
  }

  implicit private def features = zuoraService.getFeatures

  override def createPaidSubscription(contactId: ContactId,
                                      identityId: String,
                                      joinData: PaidMemberForm,
                                      nameData: NameForm,
                                      tier: PaidTier,
                                      email: String,
                                      paymentMethod: PaymentMethod,
                                      ipCountry: Option[Country]): Future[SubscribeResult] = {

    val transactingCountry = joinData.billingAddress.flatMap(_.country) orElse joinData.zuoraAccountAddress.country orElse ipCountry getOrElse UK

    val subscribe = zuoraService.getFeatures.map { features =>
      val planChoice = PaidPlanChoice(tier, joinData.payment.billingPeriod)
      val planId = planChoice.productRatePlanId
      val plan = RatePlan(productRatePlanId = planId.get,chargeOverride = None, featuresPerTier(features)(planId, joinData.featureChoice).map(_.id.get))
      val currency = catalog.unsafeFindPaid(planId).currencyOrGBP(transactingCountry)
      val today = DateTime.now.toLocalDate

      Subscribe(account = createAccount(contactId, identityId, currency, joinData.payment, transactingCountry),
        paymentMethod = Some(paymentMethod),
        address = joinData.zuoraAccountAddress,
        email = email,
        ratePlans = NonEmptyList(plan),
        name = nameData,
        ipCountry = ipCountry,
        contractAcceptance = today,
        contractEffective = today)
    }.andThen { case Failure(e) => logger.error(s"Could not get features in tier ${tier.name} for user with salesforceContactId ${contactId.salesforceContactId}", e) }

    subscribe
      .flatMap(zuoraService.createSubscription)
      .andThen {
        case Success(_) => salesforceService.metrics.putCreationOfPaidSubscription(paymentMethod)
        case Failure(e) => logger.error(s"Could not create paid subscription in tier ${tier.name} for user with salesforceContactId ${contactId.salesforceContactId} for transacting country: $transactingCountry", e)
      }
  }

  private def createAccount(contactId: ContactId, identityId: String, currency: Currency, paymentForm: CommonPaymentForm, transactingCountry: Country) =
    paymentForm match {
      case PaymentForm(_, Some(_), _) | MonthlyPaymentForm(Some(_), _, _) =>
        val paymentGateway = if (transactingCountry == Australia) auStripeService.paymentGateway else ukStripeService.paymentGateway
        Account(contactId, identityId, currency, autopay = true, paymentGateway)
      case PaymentForm(_, _, Some(_)) | MonthlyPaymentForm(_, Some(_), _) =>
        Account(contactId, identityId, currency, autopay = true, PayPal)
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
    if (items.isEmpty)
      items
    else {
      val sortedItems = items.sortBy(_.chargeNumber)
      sortedItems.filter(_.subscriptionId == sortedItems.last.subscriptionId)
    }
  }

  /**
    * Construct an Amend command (used for subscription upgrades)
    */
  private def amend(sub: Subscription[SubscriptionPlan.Member], planChoice: PlanChoice, form: Set[FeatureChoice])
                   (implicit r: IdentityRequest): Future[Amend] = {

    val newPlan = catalog.unsafeFindPaid(planChoice.productRatePlanId)
    val tier = newPlan.tier

    val ids = subscriptionService.backdoorRatePlanIds(sub.name).map(_.fold({ error =>
      throw new Exception(s"REST sub not found for ${sub.id}: $error")
    }, identity))

    val zuoraFeatures = zuoraService.getFeatures.map { fs => featureIdsForTier(fs)(tier, form) }
    val newRatePlan = zuoraFeatures.map(fs => RatePlan(newPlan.id.get, None, fs.map(_.get)))
    val currentRatePlan = sub.plan.id
    logger.info(s"Current Rate Plan is: ${sub.plan.productName}. Plan to remove is: $currentRatePlan")

    (newRatePlan |@| ids) { case (newPln, restSub) =>
      val discountsToRemove = getDiscountRatePlanIdsToRemove(restSub, discountIds)
      if (discountsToRemove.nonEmpty) logger.info(s"Discount Rate Plan ids to remove when upgrading are: $discountsToRemove")
      val plansToRemove = currentRatePlan +: discountsToRemove
      if (plansToRemove.isEmpty) logger.error(s"plansToRemove is empty - this could lead to overlapping rate plans on the Zuora sub: ${sub.id}")
      Amend(sub.id.get, plansToRemove.map(_.get), NonEmptyList(newPln), None)
    }
  }

  private def upgradeSubscription(sub: Subscription[SubscriptionPlan.Member],
                                  contact: Contact,
                                  planChoice: PlanChoice,
                                  form: MemberChangeForm,
                                  referralData: ReferralData)(implicit r: IdentityRequest): Future[MemberError \/ ContactId] = {

    val addressDetails = form.addressDetails
    val newPlan = catalog.unsafeFindPaid(planChoice.productRatePlanId)
    val tier = newPlan.tier

    (for {
      s <- EitherT(Future.successful(subOrPendingAmendError(sub)))
      country <- EitherT(country(contact).map(\/.right))
      command <- EitherT(amend(sub, planChoice, form.featureChoice).map(\/.right))
      _ <- zuoraService.upgradeSubscription(command).liftM
      _ <- salesforceService.updateMemberStatus(IdMinimalUser(contact.identityId, None), newPlan.tier, None).liftM
    } yield {
      salesforceService.metrics.putUpgrade(tier)
      addressDetails.foreach(identityService.updateUserFieldsBasedOnUpgrade(contact.identityId, _))
      trackUpgrade(contact, sub, newPlan, addressDetails, referralData)
      contact
    }).run
  }

  private def createPaymentMethod(sub: FreeMember, form: FreeMemberChangeForm): Future[UpdateResult] = {
    val transactingCountry = form.billingAddress.flatMap(_.country) orElse form.deliveryAddress.country getOrElse UK
    form.payment match {
      case PaymentForm(_, Some(stripeToken), _) =>
        createStripePaymentMethod(sub.contact, stripeToken, transactingCountry)
      case PaymentForm(_, _, Some(payPalBaid)) =>
        createPayPalPaymentMethod(sub.contact, payPalBaid)
    }
  }

  private def createStripePaymentMethod(contact: Contact,
                                        stripeToken: String,
                                        transactingCountry: Country): Future[UpdateResult] = {
    val stripeService = if (transactingCountry == Australia) auStripeService else ukStripeService
    for {
      customer <- stripeService.Customer.create(contact.identityId, stripeToken)
      sub <- subscriptionService.current[SubscriptionPlan.Member](contact).map(_.head)
      result <- zuoraService.createCreditCardPaymentMethod(sub.accountId, customer, stripeService.paymentGateway)
    } yield result
  }

  private def createPayPalPaymentMethod(contact: Contact,
                                        payPalBaid: String): Future[UpdateResult] =
    for {
      sub <- subscriptionService.current[SubscriptionPlan.Member](contact).map(_.head)
      maybeEmail <- zuoraRestService.getAccount(sub.accountId) map (_.toOption.flatMap(_.billToContact.email))
      result <- zuoraService.createPayPalPaymentMethod(sub.accountId, payPalBaid, maybeEmail.getOrElse(""))
    } yield result

  private def subOrPendingAmendError[P <: Subscription[SubscriptionPlan.Member]](sub: P): MemberError \/ P =
    if (sub.hasPendingFreePlan || sub.isCancelled)
      PendingAmendError(sub.name).left
    else
      sub.right

}
