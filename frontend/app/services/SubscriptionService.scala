package services

import com.github.nscala_time.time.Imports._
import com.gu.config.Membership
import com.gu.i18n.{CountryGroup, Country, Currency, GBP}
import com.gu.membership.model._
import com.gu.membership.salesforce.Tier._
import com.gu.membership.salesforce._
import com.gu.membership.stripe.Stripe
import com.gu.membership.touchpoint.TouchpointBackendConfig.BackendType
import com.gu.membership.util.FutureSupplier
import com.gu.membership.zuora.soap.Readers._
import com.gu.membership.zuora.soap._
import com.gu.membership.zuora.soap.actions.Actions._
import com.gu.membership.zuora.soap.actions.subscribe
import com.gu.membership.zuora.soap.actions.subscribe.Subscribe
import com.gu.membership.zuora.soap.models.Queries.{PreviewInvoiceItem, Amendment}
import com.gu.membership.zuora.soap.models.Results._
import com.gu.membership.zuora.soap.models.{Queries => Soap, _}
import com.gu.membership.zuora.{rest, soap}
import com.gu.monitoring.ServiceMetrics
import com.gu.services.PaymentService
import com.typesafe.scalalogging.LazyLogging
import forms.MemberForm.{PaidMemberJoinForm, JoinForm}
import model._
import org.joda.time.DateTime
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import views.support.ThankyouSummary
import views.support.ThankyouSummary.NextPayment
import scalaz.syntax.applicative._
import scalaz.std.option._

import scala.concurrent.Future

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
  def findCurrentSubscriptionStatus(subscriptionVersions: Seq[Soap.Subscription], amendments: Seq[Soap.Amendment]): SubscriptionStatus = {
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
  def latestInvoiceItems(items: Seq[Soap.InvoiceItem]): Seq[Soap.InvoiceItem] = {
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
  def sortAmendments(subscriptions: Seq[Soap.Subscription], amendments: Seq[Soap.Amendment]): Seq[Amendment] = {
    val versionsNumberBySubVersionId = subscriptions.map { sub => (sub.id, sub.version) }.toMap
    amendments.sortBy { amendment => versionsNumberBySubVersionId(amendment.subscriptionId) }
  }

  def sortPreviewInvoiceItems(items: Seq[Soap.PreviewInvoiceItem]): Seq[PreviewInvoiceItem] = items.sortBy(_.price)

  def sortSubscriptions(subscriptions: Seq[Soap.Subscription]): Seq[Soap.Subscription] = subscriptions.sortBy(_.version)

  def featuresPerTier(zuoraFeatures: Seq[Soap.Feature])(plan: TierPlan, choice: Set[FeatureChoice]): Seq[Soap.Feature] = {
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
			                    val paymentService: PaymentService) extends LazyLogging {

  import SubscriptionService._

  implicit private val _bt = bt

  val membershipCatalog: FutureSupplier[MembershipCatalog] = new FutureSupplier[MembershipCatalog](
    productRatePlans.map(MembershipCatalog.unsafeFromZuora(productFamily))
  )

  def productRatePlans: Future[Seq[rest.ProductRatePlan]] =
    zuoraRestClient.productCatalog.map(_.products.flatMap(_.productRatePlans))

  def getMembershipCatalog: Future[MembershipCatalog.Val[MembershipCatalog]] =
    productRatePlans.map(MembershipCatalog.fromZuora(productFamily))

  private def subscriptionVersions(subscriptionNumber: String): Future[Seq[Soap.Subscription]] = for {
    subscriptions <- zuoraSoapClient.query[Soap.Subscription](SimpleFilter("Name", subscriptionNumber))
  } yield subscriptions


  def currentSubscription(contact: ContactId): Future[model.Subscription] = for {
    catalog <- membershipCatalog.get()
    accounts <- zuoraSoapClient.query[Soap.Account](SimpleFilter("crmId", contact.salesforceAccountId))
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
    amendments <- zuoraSoapClient.query[Soap.Amendment](OrFilter(subscriptionVersions.map(s => ("SubscriptionId", s.id)): _*))
  } yield findCurrentSubscriptionStatus(subscriptionVersions, amendments)

  /*
   * If the member is entitled to complimentary tickets return its Zuora account's corresponding usage records count.
   * Returns none otherwise
   */
  def getUsageCountWithinTerm(subscription: model.Subscription, unitOfMeasure: String): Future[Option[Int]] = {
    val features = subscription.features
    //TODO: review date formats here
    val startDate = DateTimeHelpers.formatDateTime(subscription.startDate.toDateTimeAtCurrentTime)

    val usageCountF = zuoraSoapClient.query[Soap.Usage](AndFilter(("StartDateTime", startDate),
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
    paymentMethod <- zuoraSoapClient.authenticatedRequest(CreatePaymentMethod(sub.accountId, customer))
    result <- zuoraSoapClient.authenticatedRequest(EnablePayment(sub.accountId, paymentMethod.id))
  } yield result

  def createFreeSubscription(memberId: ContactId, joinData: JoinForm): Future[SubscribeResult] =
    for {
      zuoraFeatures <- zuoraSoapClient.featuresSupplier.get()
      ratePlanId <- findRatePlanId(joinData.plan)
      result <- zuoraSoapClient.authenticatedRequest(Subscribe(
        account = subscribe.Account.stripe(memberId, GBP, autopay = false),
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
        account = subscribe.Account.stripe(memberId,
          currency = supportedAccountCurrency(catalog)(joinData.zuoraAccountAddress.country, joinData.plan),
          autopay = true),
        paymentMethodOpt = Some(subscribe.CreditCardReferenceTransaction(customer)),
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
      invoiceItems <- zuoraSoapClient.query[Soap.InvoiceItem](SimpleFilter("SubscriptionNumber", subscription.number))
    } yield {
      val filteredInvoices = latestInvoiceItems(invoiceItems)
      PaymentSummary(filteredInvoices, subscription.accountCurrency)
    }
  }

  def getMembershipSubscriptionSummary(contact: Contact[MemberStatus, PaymentMethod]): Future[ThankyouSummary] = {
    val latestSubF = currentSubscription(contact)
    def price(amount: Float)(implicit currency: Currency) = Price(amount.toInt, currency)
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
          renewalDate = payment.current.nextPaymentDate,
          initialFreePeriodOffer = false,
          billingPeriod = bp
        )
      }

    def getSummaryViaPreview =
      for {
        sub <- latestSubF
        pd <- paymentService.paymentDetails(contact, productFamily)
      } yield {
        implicit val currency = sub.accountCurrency
        val (planAmount, bp) = plan(sub)
        def price(amount: Float) = Price(amount.toInt, sub.accountCurrency)
        val nextPayment = (pd.nextPaymentPrice.map(price) |@| pd.nextPaymentDate) { NextPayment }

        ThankyouSummary(
          startDate = sub.startDate.toDateTimeAtCurrentTime(),
          amountPaidToday = price(0f),
          planAmount = planAmount,
          nextPayment = nextPayment,
          renewalDate = pd.termEndDate.plusDays(1),
          sub.isInTrialPeriod,
          bp
        )
      }

    for {
      userInvoiced <- latestSubF.map(_.userHasBeenInvoiced)
      summary <- if (userInvoiced) getSummaryViaInvoice else getSummaryViaPreview
    } yield summary
  }

  def subscriptionUpgradableTo(memberId: Contact[Member, PaymentMethod], targetTier: PaidTier): Future[Option[model.Subscription]] = {
    import model.TierOrdering.upgradeOrdering

    membershipCatalog.get().zip(currentSubscription(memberId)).map { case (catalog, sub) =>
      val currentTier = memberId.tier
      val targetCurrencies = catalog.paidTierDetails(targetTier).currencies

      if (!sub.isInTrialPeriod && targetCurrencies.contains(sub.accountCurrency) && targetTier > currentTier) {
        Some(sub)
      } else None
    }
  }

  def cancelSubscription(contactId: ContactId): Future[AmendResult] =
    for {
      sub <- subWithNoPendingAmend(contactId)
      cancelDate = sub match {
        case p: PaidSubscription => p.chargedThroughDate.map(_.toDateTimeAtCurrentTime).getOrElse(DateTime.now)
        case _ => DateTime.now
      }
      result <- zuoraSoapClient.authenticatedRequest(CancelPlan(sub.id, sub.ratePlanId, cancelDate))
    } yield result

  def downgradeSubscription(contactId: ContactId): Future[AmendResult] = {
    //if the member has paid upfront so they should have the higher tier until charged date has completed then be downgraded
    //otherwise use customer acceptance date (which should be in the future)
    def effectiveFrom(sub: model.PaidSubscription) = sub.chargedThroughDate.getOrElse(sub.firstPaymentDate).toDateTimeAtCurrentTime

    for {
      sub <- subWithNoPendingAmend(contactId)
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
    } yield result
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

  def getSubscriptionsByCasId(casId: String): Future[Seq[Soap.Subscription]] =
    zuoraSoapClient.query[Soap.Subscription](SimpleFilter("CASSubscriberID__c", casId))
}
