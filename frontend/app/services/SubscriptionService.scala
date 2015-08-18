package services

import com.github.nscala_time.time.Imports._
import com.gu.membership.model._
import com.gu.membership.salesforce.MemberId
import com.gu.membership.salesforce.Tier.{Partner, Patron}
import com.gu.membership.stripe.Stripe
import com.typesafe.scalalogging.LazyLogging
import forms.MemberForm.JoinForm
import model.{FeatureChoice, MembershipSummary}
import model.Zuora._
import model.ZuoraDeserializer._
import org.joda.time.DateTime
import services.zuora._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class SubscriptionServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}

trait AmendSubscription {
  self: SubscriptionService =>

  private def checkForPendingAmendments(memberId: MemberId)(fn: String => Future[AmendResult]): Future[AmendResult] = {
    getSubscriptionStatus(memberId).flatMap { subscriptionStatus =>
      if (subscriptionStatus.future.isEmpty) {
        fn(subscriptionStatus.current)
      } else {
        throw SubscriptionServiceError("Cannot amend subscription, amendments are already pending")
      }
    }
  }

  def cancelSubscription(memberId: MemberId, instant: Boolean): Future[AmendResult] = {
    checkForPendingAmendments(memberId) { subscriptionId =>
      for {
        subscriptionDetails <- getSubscriptionDetails(subscriptionId)
        cancelDate = if (instant) DateTime.now else subscriptionDetails.chargedThroughDate.getOrElse(DateTime.now())
        result <- zuoraSoapService.authenticatedRequest(CancelPlan(subscriptionId, subscriptionDetails.ratePlanId, cancelDate))
      } yield result
    }
  }

  def downgradeSubscription(memberId: MemberId, newTierPlan: TierPlan): Future[AmendResult] = {

    //if the member has paid upfront so they should have the higher tier until charged date has completed then be downgraded
    //otherwise use customer acceptance date (which should be in the future)
    def effectiveFrom(subscriptionDetails: SubscriptionDetails) = subscriptionDetails.chargedThroughDate.getOrElse(subscriptionDetails.contractAcceptanceDate)


    checkForPendingAmendments(memberId) { subscriptionId =>
      for {
        subscriptionDetails <- getSubscriptionDetails(subscriptionId)
        dateToMakeDowngradeEffectiveFrom = effectiveFrom(subscriptionDetails)

        result <- zuoraSoapService.authenticatedRequest(DowngradePlan(subscriptionId, subscriptionDetails.ratePlanId,
          tierPlanRateIds(newTierPlan), dateToMakeDowngradeEffectiveFrom))
      } yield result
    }
  }

  def upgradeSubscription(memberId: MemberId, newTierPlan: TierPlan, preview: Boolean): Future[AmendResult] = {
    checkForPendingAmendments(memberId) { subscriptionId =>
      for {
        ratePlan <- zuoraSoapService.queryOne[RatePlan](s"SubscriptionId='$subscriptionId'")
        result <- zuoraSoapService.authenticatedRequest(UpgradePlan(subscriptionId, ratePlan.id, tierPlanRateIds(newTierPlan), preview))
      } yield result
    }
  }
}

object SubscriptionService {
  def sortAmendments(subscriptions: Seq[Subscription], amendments: Seq[Amendment]) = {
    val versions = subscriptions.map { amendment => (amendment.id, amendment.version) }.toMap
    amendments.sortBy { amendment => versions(amendment.subscriptionId) }
  }

  def sortInvoiceItems(items: Seq[InvoiceItem]) = items.sortBy(_.chargeNumber)

  def sortPreviewInvoiceItems(items: Seq[PreviewInvoiceItem]) = items.sortBy(_.price)

  def sortSubscriptions(subscriptions: Seq[Subscription]) = subscriptions.sortBy(_.version)

  def sortAccounts(accounts: Seq[Account]) = accounts.sortBy(_.createdDate)

  def featuresPerTier(zuoraFeatures: Seq[Feature])(tier: ProductRatePlan, choice: Set[FeatureChoice]): Seq[Feature] = {
    def byChoice(choice: Set[FeatureChoice]) =
      zuoraFeatures.filter(f => choice.map(_.zuoraCode).contains(f.code))

    tier match {
      case PaidTierPlan(Patron, _) => byChoice(FeatureChoice.all)
      case PaidTierPlan(Partner, _) => byChoice(choice).take(1)
      case _ => Seq[Feature]()
    }
  }
}

class SubscriptionService(val tierPlanRateIds: Map[ProductRatePlan, String],
                          val zuoraSoapService: ZuoraSoapService,
                          val zuoraRestService: ZuoraRestService) extends AmendSubscription with LazyLogging {
  import SubscriptionService._

  def getAccount(memberId: MemberId): Future[Account] =
    zuoraSoapService.query[Account](s"crmId='${memberId.salesforceAccountId}'").map(sortAccounts(_).last)

  //TODO before we do subs we need to filter by rate plans membership knows about
  def getSubscriptions(memberId: MemberId): Future[Seq[Subscription]] = {
    for {
      account <- getAccount(memberId)
      subscriptions <- zuoraSoapService.query[Subscription](s"AccountId='${account.id}'")
    } yield subscriptions
  }

  def memberTierFeatures(memberId: MemberId): Future[Seq[Feature]] =
    for {
      account <- getAccount(memberId)
      features <- zuoraRestService.productFeaturesByAccount(account.id)
    } yield features

  def getSubscription(subscriptionId: String): Future[Subscription] = zuoraSoapService.queryOne[Subscription](s"Id='$subscriptionId'")

  def getSubscriptionStatus(memberId: MemberId): Future[SubscriptionStatus] = {
    for {
      subscriptions <- getSubscriptions(memberId)

      if subscriptions.nonEmpty

      where = subscriptions.map { sub => s"SubscriptionId='${sub.id}'" }.mkString(" OR ")
      amendments <- zuoraSoapService.query[Amendment](where)
    } yield {
      val latestSubscriptionId = sortSubscriptions(subscriptions).last.id

      sortAmendments(subscriptions, amendments)
        .find(_.contractEffectiveDate.isAfterNow)
        .fold(SubscriptionStatus(latestSubscriptionId, None, None)) { amendment =>
          SubscriptionStatus(amendment.subscriptionId, Some(latestSubscriptionId), Some(amendment.amendType))
        }
    }
  }

  def getSubscriptionDetails(subscriptionId: String): Future[SubscriptionDetails] = {
    for {
      subscription <- getSubscription(subscriptionId)
      ratePlan <- zuoraSoapService.queryOne[RatePlan](s"SubscriptionId='$subscriptionId'")
      ratePlanCharge <- zuoraSoapService.queryOne[RatePlanCharge](s"RatePlanId='${ratePlan.id}'")
    } yield SubscriptionDetails(subscription, ratePlan, ratePlanCharge)
  }

  def getCurrentSubscriptionDetails(memberId: MemberId): Future[SubscriptionDetails] = {
    for {
      subscriptionStatus <- getSubscriptionStatus(memberId)
      subscriptionDetails <- getSubscriptionDetails(subscriptionStatus.current)
    } yield subscriptionDetails
  }

  def createPaymentMethod(memberId: MemberId, customer: Stripe.Customer): Future[UpdateResult] = {
    for {
      account <- getAccount(memberId)
      paymentMethod <- zuoraSoapService.authenticatedRequest(CreatePaymentMethod(account, customer))
      result <- zuoraSoapService.authenticatedRequest(EnablePayment(account, paymentMethod))
    } yield result
  }

  def createSubscription(memberId: MemberId,
                         joinData: JoinForm,
                         customerOpt: Option[Stripe.Customer],
                         paymentDelay: Option[Period],
                         casId: Option[String]): Future[SubscribeResult] = for {

      zuoraFeatures <- zuoraSoapService.featuresSupplier.get()
      features = featuresPerTier(zuoraFeatures)(joinData.plan, joinData.featureChoice)
      result <- zuoraSoapService.authenticatedRequest(Subscribe(memberId,
                                            customerOpt,
                                            tierPlanRateIds(joinData.plan),
                                            joinData.name,
                                            joinData.deliveryAddress,
                                            paymentDelay,
                                            casId,
                                            features))
    } yield result

  def getPaymentSummary(memberId: MemberId): Future[PaymentSummary] = {
    for {
      subscription <- getSubscriptionStatus(memberId)
      invoiceItems <- zuoraSoapService.query[InvoiceItem](s"SubscriptionId='${subscription.current}'")
    } yield PaymentSummary(invoiceItems)
  }

  def getMembershipSubscriptionSummary(memberId: MemberId): Future[MembershipSummary] = {

    val latestSubF = {
      for (subscriptions <- getSubscriptions(memberId))
      yield sortSubscriptions(subscriptions).last
    }

    def hasUserBeenInvoiced(memberId: MemberId) =
      for (subscription <- latestSubF)
      yield subscription.contractAcceptanceDate.isAfterNow

    def getSummaryViaSubscriptionAmend(memberId: MemberId) = {
      for {
        latestSubscription <- latestSubF
        subscriptionDetailsF = getSubscriptionDetails(latestSubscription.id)
        result <- zuoraSoapService.authenticatedRequest(SubscriptionDetailsViaAmend(latestSubscription.id, latestSubscription.contractAcceptanceDate))
        subscriptionDetails <- subscriptionDetailsF
      } yield {
        assert(result.invoiceItems.nonEmpty, "Subscription with delayed payment returning zero invoice items in SubscriptionDetailsViaAmend call")
        val firstPreviewInvoice = result.invoiceItems.sortBy(_.serviceStartDate).head

        MembershipSummary(latestSubscription.termStartDate, firstPreviewInvoice.serviceEndDate, None,
          subscriptionDetails.planAmount, firstPreviewInvoice.price, firstPreviewInvoice.serviceStartDate, firstPreviewInvoice.renewalDate )
      }
    }

    def getSummaryViaInvoice(memberId: MemberId) = for (paymentSummary <- getPaymentSummary(memberId)) yield {
      MembershipSummary(paymentSummary.current.serviceStartDate, paymentSummary.current.serviceEndDate,
        Some(paymentSummary.totalPrice), paymentSummary.current.price, paymentSummary.current.price, paymentSummary.current.nextPaymentDate, paymentSummary.current.nextPaymentDate)
    }

    for {
      userInvoiced <- hasUserBeenInvoiced(memberId)
      summary <- if (userInvoiced) getSummaryViaSubscriptionAmend(memberId) else getSummaryViaInvoice(memberId)
    } yield summary
  }

  def getSubscriptionsByCasId(casId: String): Future[Seq[Subscription]] =  zuoraSoapService.query[Subscription](s"CASSubscriberID__c='$casId'")
}
