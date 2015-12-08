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

trait MemberService extends LazyLogging with ActivityTracking {
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


  def getStripeCustomer(contact: GenericSFContact): Future[Option[Customer]] = contact.paymentMethod match {
    case StripePayment(id) =>
      TouchpointBackend.forUser(contact).stripeService.Customer.read(id).map(Some(_))
    case _ =>
      Future.successful(None)
  }
}


case class SubscriptionServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}

object SubscriptionService {
  val membershipProductType = "Membership"
  val productRatePlanChargeModel = "FlatFee"

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
}
