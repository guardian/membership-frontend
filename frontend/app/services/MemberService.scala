package services

import com.github.nscala_time.time.Imports._
import com.gu.i18n.{Country, CountryGroup, Currency, GBP}
import com.gu.identity.play.IdMinimalUser
import com.gu.membership.model._
import com.gu.membership.util.Timing
import com.gu.memsub.services.api.PaymentService
import com.gu.salesforce.Tier.{Partner, Patron}
import com.gu.salesforce.{ContactId, PaidTier}
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
import model._
import org.joda.time.DateTime
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import tracking._
import views.support.ThankyouSummary
import views.support.ThankyouSummary.NextPayment

import scala.concurrent.Future
import scala.util.{Failure, Success}

case class MemberServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}

object MemberService {
  def featureIdsForTier(features: Seq[SoapQueries.Feature])(plan: TierPlan, choice: Set[FeatureChoice]): Seq[FeatureId] = {
    def chooseFeature(choices: Set[FeatureChoice]): Seq[FeatureId] =
      features.filter(f => choices.map(_.zuoraCode).contains(f.code))
        .map(_.id)

    plan.tier match {
      case Patron => chooseFeature(FeatureChoice.all)
      case Partner => chooseFeature(choice).take(1)
      case _ => Nil
    }
  }
}

class MemberService(identityService: IdentityService,
                     salesforceService: api.SalesforceService,
                     zuoraService: ZuoraService,
                     stripeService: StripeService,
                     catalogService: api.CatalogService,
                     paymentService: PaymentService) extends api.MemberService with ActivityTracking {


  import EventbriteService._
  import MemberService.featureIdsForTier

  private val logger = Logger(getClass)

  override def currentSubscription(contactId: ContactId): Future[model.Subscription] =
    for {
      cat <- catalogService.membershipCatalog.get()
      accounts <- zuoraService.getAccounts(contactId)
      accountAndSubscriptionOpts <- Future.traverse(accounts) { account =>
        zuoraService.getLatestRestSubscription(catalogService.productFamily, account).map(account -> _)
      }
    } yield {
      val (account, restSub) =
        accountAndSubscriptionOpts.collect { case (acc, Some(subscription)) =>
          acc -> subscription
        }.sortBy(_._2.termStartDate).lastOption.getOrElse(throw new MemberServiceError(
          s"Cannot find a membership subscription for account ids ${accounts.map(_.id)}"))

      model.Subscription(cat)(contactId, account, restSub)
    }

  override def currentPaidSubscription(contact: ContactId): Future[model.PaidSubscription] =
    currentSubscription(contact).map {
      case paid: PaidSubscription => paid
      case sub =>
        throw MemberServiceError(s"Expecting subscription ${sub.number} to be paid, got a free one instead (tier: ${sub.plan})")
    }

  override def createMember(user: IdMinimalUser,
                            formData: JoinForm,
                            identityRequest: IdentityRequest,
                            fromEventId: Option[String]): Future[ContactId] = {

    val tier = formData.plan.tier

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
            subscription <- createPaidSubscription(cId, paid, customer)
            updatedMember <- salesforceService.updateMemberStatus(user, formData.plan.tier, Some(customer))
          } yield cId
        case _ =>
          for {
            cId <- createContact
            subscription <- createFreeSubscription(cId, formData)
            updatedMember <- salesforceService.updateMemberStatus(user, formData.plan.tier, None)
          } yield cId
      }

      contactId.map { cId =>
        identityService.updateUserFieldsBasedOnJoining(user, formData, identityRequest)

        salesforceService.metrics.putSignUp(formData.plan)
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
        salesforceService.metrics.putFailSignUp(formData.plan)
    }
  }

  override def upgradeFreeSubscription(freeMember: FreeSFMember,
                                       newTier: PaidTier,
                                       form: FreeMemberChangeForm,
                                       identityRequest: IdentityRequest): Future[ContactId] = {
    val plan = PaidTierPlan(newTier, form.payment.billingPeriod, Current)
    for {
      customer <- stripeService.Customer.create(freeMember.identityId, form.payment.token)
      paymentResult <- createPaymentMethod(freeMember, customer)
      memberId <- upgradeSubscription(freeMember, plan, form, Some(customer), identityRequest)
    } yield {

      memberId
    }
  }

  override def upgradePaidSubscription(paidMember: PaidSFMember,
                              newTier: PaidTier,
                              form: PaidMemberChangeForm,
                              identityRequest: IdentityRequest): Future[ContactId] =
    for {
      subs <- currentPaidSubscription(paidMember)
      cat <- catalogService.membershipCatalog.get()
      currentPlan = cat.unsafePaidTierPlan(subs.productRatePlanId)
      newPlan = PaidTierPlan(newTier, currentPlan.billingPeriod, status = Current)
      memberId <- upgradeSubscription(paidMember, newPlan, form, None, identityRequest)
    } yield memberId

  override def downgradeSubscription(contact: SFMember, user: IdMinimalUser): Future[String] = {
    //if the member has paid upfront so they should have the higher tier until charged date has completed then be downgraded
    //otherwise use customer acceptance date (which should be in the future)
    def effectiveFrom(sub: model.PaidSubscription): DateTime = sub.chargedThroughDate.getOrElse(sub.firstPaymentDate).toDateTimeAtCurrentTime

    for {
      sub <- subWithNoPendingAmend(contact)
      paidSub = sub match {
        case p: PaidSubscription => p
        case _ => throw MemberServiceError(s"Expected to downgrade a paid subscription")
      }
      friendRatePlanId <- catalogService.findProductRatePlanId(FriendTierPlan.current)
      result <- zuoraService.downgradePlan(
        subscriptionId = paidSub.id,
        currentRatePlanId = paidSub.ratePlanId,
        futureRatePlanId = friendRatePlanId,
        effectiveFrom = effectiveFrom(paidSub))
    } yield {
      salesforceService.metrics.putDowngrade(contact.tier)
      track(
        MemberActivity(
          "downgradeMembership",
          MemberData(
            salesforceContactId = contact.salesforceContactId,
            identityId = contact.identityId,
            tier = contact.tier.name,
            tierAmendment = Some(DowngradeAmendment(contact.tier)) //getting effective date and subscription annual / month is proving difficult
          )),
        user)

      ""
    }
  }

  override def cancelSubscription(contact: SFMember, user: IdMinimalUser): Future[String] = {
    for {
      sub <- subWithNoPendingAmend(contact)
      cancelDate = sub match {
        case p: PaidSubscription => p.chargedThroughDate.map(_.toDateTimeAtCurrentTime).getOrElse(DateTime.now)
        case _ => DateTime.now
      }
      _ <- zuoraService.cancelPlan(sub.id, sub.ratePlanId, cancelDate)
    } yield {
      salesforceService.metrics.putCancel(contact.tier)
      track(MemberActivity("cancelMembership", MemberData(contact.salesforceContactId, contact.identityId, contact.tier.name)), user)
      ""
    }
  }

  override def previewUpgradeSubscription(subscription: model.PaidSubscription,
                                 newTier: PaidTier): Future[Seq[PreviewInvoiceItem]] = {
    val newTierPlan = PaidTierPlan(newTier, subscription.plan.billingPeriod, Current)

    for {
      newRatePlanId <- catalogService.findProductRatePlanId(newTierPlan)
      featureIds <- zuoraService.getFeatures.map { fs =>
        featureIdsForTier(fs)(newTierPlan, Set.empty)
      }
      result <- zuoraService.upgradeSubscription(subscription.id, subscription.ratePlanId, newRatePlanId, featureIds, preview = false)
    } yield result.invoiceItems
  }

  override def subscriptionUpgradableTo(memberId: SFMember, targetTier: PaidTier): Future[Option[model.Subscription]] = {
    import model.TierOrdering.upgradeOrdering

    catalogService.membershipCatalog.get().zip(currentSubscription(memberId)).map { case (catalog, sub) =>
      val currentTier = memberId.tier
      val targetCurrencies = catalog.paidTierDetails(targetTier).currencies

      if (!sub.isInTrialPeriod && targetCurrencies.contains(sub.accountCurrency) && targetTier > currentTier) {
        Some(sub)
      } else None
    }
  }

  override def updateDefaultCard(member: PaidSFMember, token: String): Future[Stripe.Card] =
    for {
      customer <- stripeService.Customer.updateCard(member.stripeCustomerId, token)
      memberId <- salesforceService.updateCardId(member.identityId, customer.card.id)
    } yield customer.card

  override  def getMembershipSubscriptionSummary(contact: GenericSFContact): Future[ThankyouSummary] = {
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
        paymentDetails <- paymentService.paymentDetails(contact, catalogService.productFamily)
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

  override def getUsageCountWithinTerm(subscription: model.Subscription, unitOfMeasure: String): Future[Option[Int]] = {
    val features = subscription.features
    val startDate = subscription.startDate.toDateTimeAtCurrentTime()
    zuoraService.getUsages(subscription.number, unitOfMeasure, startDate).map { usages =>
      val hasComplimentaryTickets = features.contains(FreeEventTickets)
      if (!hasComplimentaryTickets) None else Some(usages.size)
    }
  }

  override def recordFreeEventUsage(member: SFMember, event: RichEvent, order: EBOrder, quantity: Int): Future[CreateResult] = {
    val description = s"event-id:${event.id};order-id:${order.id}"

    for {
      subs <- currentSubscription(member)
      result <- zuoraService.createFreeEventUsage(
        accountId = subs.accountId,
        subscriptionNumber = subs.number,
        description = description,
        quantity = quantity
      )
    } yield {
      logger.info(s"Recorded a complimentary event ticket usage for account ${subs.accountId}, subscription: ${subs.number}, details: $description")
      result
    }
  }

  override def retrieveComplimentaryTickets(member: SFMember, event: RichEvent): Future[Seq[EBTicketClass]] = {
    Timing.record(salesforceService.metrics, "retrieveComplimentaryTickets") {
      for {
        subs <- currentSubscription(member)
        usageCount <- getUsageCountWithinTerm(subs, FreeEventTickets.unitOfMeasure)
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

  override def createEBCode(member: SFMember, event: RichEvent): Future[Option[EBCode]] = {
    retrieveComplimentaryTickets(member, event).flatMap { complimentaryTickets =>
      val code = DiscountCode.generate(s"A_${member.identityId}_${event.id}")
      val unlockedTickets = complimentaryTickets ++ event.retrieveDiscountedTickets(member.tier)
      event.service.createOrGetAccessCode(event, code, unlockedTickets)
    }
  }

  override def createFreeSubscription(contactId: ContactId,
                                      joinData: JoinForm): Future[SubscribeResult] =
    for {
      zuoraFeatures <- zuoraService.getFeatures
      productRatePlanId <- catalogService.findProductRatePlanId(joinData.plan)
      result <- zuoraService.createSubscription(
        subscribeAccount = SoapSubscribeAccount.stripe(contactId, GBP, autopay = false),
        paymentMethod = None,
        productRatePlanId = productRatePlanId,
        name = joinData.name,
        address = joinData.deliveryAddress
      )
    } yield result

  implicit private def features = zuoraService.getFeatures

  override def createPaidSubscription(contactId: ContactId,
                                      joinData: PaidMemberJoinForm,
                                      customer: Stripe.Customer): Future[SubscribeResult] =
    for {
      catalog <- catalogService.membershipCatalog.get()
      zuoraFeatures <- zuoraService.getFeatures
      productRatePlanId <- catalogService.findProductRatePlanId(joinData.plan)
      result <- zuoraService.createSubscription(
        subscribeAccount = SoapSubscribeAccount.stripe(
          contactId = contactId,
          currency = supportedAccountCurrency(catalog)(joinData.zuoraAccountAddress.country, joinData.plan),
          autopay = true),
        paymentMethod = Some(CreditCardReferenceTransaction(customer)),
        productRatePlanId = productRatePlanId,
        name = joinData.name,
        address = joinData.zuoraAccountAddress,
        featureIds = featuresPerTier(zuoraFeatures)(joinData.plan, joinData.featureChoice).map(_.id)
      )
    } yield result


  // TODO move into catalog
  private def supportedAccountCurrency(catalog: MembershipCatalog)(country: Country, plan: PaidTierPlan): Currency =
    CountryGroup
      .byCountryCode(country.alpha2).map(_.currency)
      .filter(catalog.paidTierPlanDetails(plan).currencies)
      .getOrElse(GBP)

  private def featuresPerTier(zuoraFeatures: Seq[SoapQueries.Feature])(plan: TierPlan, choice: Set[FeatureChoice]): Seq[SoapQueries.Feature] = {
    def byChoice(choice: Set[FeatureChoice]) =
      zuoraFeatures.filter(f => choice.map(_.zuoraCode).contains(f.code))

    plan.tier match {
      case Patron => byChoice(FeatureChoice.all)
      case Partner => byChoice(choice).take(1)
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

  private def upgradeSubscription(member: SFMember,
                                  newTierPlan: PaidTierPlan,
                                  form: MemberChangeForm,
                                  customerOpt: Option[Customer],
                                  identityRequest: IdentityRequest): Future[ContactId] = {
    val addressDetails = form.addressDetails

    addressDetails.foreach(
      identityService.updateUserFieldsBasedOnUpgrade(member.identityId, _, identityRequest))

    for {
      _ <- salesforceService.updateMemberStatus(IdMinimalUser(member.identityId, None), newTierPlan.tier, customerOpt)
      sub <- subWithNoPendingAmend(member)
      newRatePlanId <- catalogService.findProductRatePlanId(newTierPlan)
      featureIds <- zuoraService.getFeatures.map { fs =>
        featureIdsForTier(fs)(newTierPlan, form.featureChoice)
      }
      _ <- zuoraService.upgradeSubscription(sub.id, sub.ratePlanId, newRatePlanId, featureIds, preview = false)
    } yield {
      salesforceService.metrics.putUpgrade(newTierPlan.tier)
      trackUpgrade(member, newTierPlan, addressDetails)
      member
    }
  }

  private def createPaymentMethod(contactId: ContactId,
                                  customer: Stripe.Customer): Future[UpdateResult] =
    for {
      sub <- currentSubscription(contactId)
      result <- zuoraService.createCreditCardPaymentMethod(sub.accountId, customer)
    } yield result

  private def subWithNoPendingAmend(contactId: ContactId): Future[model.Subscription] =
    for {
      sub <- currentSubscription(contactId)
      status <- zuoraService.getSubscriptionStatus(sub.number)
    } yield {
      if (status.futureVersionIdOpt.isEmpty) {
        sub
      } else throw MemberServiceError("Cannot amend subscription, amendments are already pending")
    }

  private def getPaymentSummary(memberId: ContactId): Future[PaymentSummary] =
    currentSubscription(memberId).flatMap { sub =>
      zuoraService.getPaymentSummary(sub.number, sub.accountCurrency)
    }
}
