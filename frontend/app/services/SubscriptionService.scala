package services

import com.github.nscala_time.time.Imports._
import com.gu.membership.salesforce.MemberId
import com.gu.membership.stripe.Stripe
import com.typesafe.scalalogging.LazyLogging
import forms.MemberForm.JoinForm
import model.Zuora._
import model.ZuoraDeserializer._
import model.{ProductRatePlan, TierPlan}
import org.joda.time.DateTime
import model.{MembershipSummary, ProductRatePlan, TierPlan}
import services.zuora._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class SubscriptionServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}

object SubscriptionServiceHelpers {
  def sortAmendments(subscriptions: Seq[Subscription], amendments: Seq[Amendment]) = {
    val versions = subscriptions.map { amendment => (amendment.id, amendment.version) }.toMap
    amendments.sortBy { amendment => versions(amendment.subscriptionId) }
  }

  def sortInvoiceItems(items: Seq[InvoiceItem]) = items.sortBy(_.chargeNumber)

  def sortPreviewInvoiceItems(items: Seq[PreviewInvoiceItem]) = items.sortBy(_.price)

  def sortSubscriptions(subscriptions: Seq[Subscription]) = subscriptions.sortBy(_.version)

  def sortAccounts(accounts: Seq[Account]) = accounts.sortBy(_.createdDate)
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
        result <- zuora.request(CancelPlan(subscriptionId, subscriptionDetails.ratePlanId, cancelDate))
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

        result <- zuora.request(DowngradePlan(subscriptionId, subscriptionDetails.ratePlanId,
          tierPlanRateIds(newTierPlan), dateToMakeDowngradeEffectiveFrom))
      } yield result
    }
  }

  def upgradeSubscription(memberId: MemberId, newTierPlan: TierPlan, preview: Boolean): Future[AmendResult] = {
    checkForPendingAmendments(memberId) { subscriptionId =>
      for {
        ratePlan <- zuora.queryOne[RatePlan](s"SubscriptionId='$subscriptionId'")
        result <- zuora.request(UpgradePlan(subscriptionId, ratePlan.id, tierPlanRateIds(newTierPlan), preview))
      } yield result
    }
  }
}

class SubscriptionService(val tierPlanRateIds: Map[ProductRatePlan, String], val zuora: ZuoraService) extends AmendSubscription with LazyLogging {
  import services.SubscriptionServiceHelpers._

  private def getAccount(memberId: MemberId): Future[Account] =
    zuora.query[Account](s"crmId='${memberId.salesforceAccountId}'").map(sortAccounts(_).last)

  //TODO before we do subs we need to filter by rate plans membership knows about
  def getSubscriptions(memberId: MemberId): Future[Seq[Subscription]] = {
    for {
      account <- getAccount(memberId)
      subscriptions <- zuora.query[Subscription](s"AccountId='${account.id}'")
    } yield subscriptions
  }

  def getSubscription(subscriptionId: String): Future[Subscription] = zuora.queryOne[Subscription](s"Id='$subscriptionId'")

  def getSubscriptionStatus(memberId: MemberId): Future[SubscriptionStatus] = {
    for {
      subscriptions <- getSubscriptions(memberId)
    
      if subscriptions.size > 0

      where = subscriptions.map { sub => s"SubscriptionId='${sub.id}'" }.mkString(" OR ")
      amendments <- zuora.query[Amendment](where)
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
      ratePlan <- zuora.queryOne[RatePlan](s"SubscriptionId='$subscriptionId'")
      ratePlanCharge <- zuora.queryOne[RatePlanCharge](s"RatePlanId='${ratePlan.id}'")
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
      paymentMethod <- zuora.request(CreatePaymentMethod(account, customer))
      result <- zuora.request(EnablePayment(account, paymentMethod))
    } yield result
  }

  def createSubscription(memberId: MemberId, joinData: JoinForm, customerOpt: Option[Stripe.Customer], paymentDelay: Option[Period], casId: Option[String]): Future[SubscribeResult] = {
    zuora.request(Subscribe(memberId, customerOpt, tierPlanRateIds(joinData.plan), joinData.name, joinData.deliveryAddress, paymentDelay, casId))
  }

  def getPaymentSummary(memberId: MemberId): Future[PaymentSummary] = {
    for {
      subscription <- getSubscriptionStatus(memberId)
      invoiceItems <- zuora.query[InvoiceItem](s"SubscriptionId='${subscription.current}'")
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
        result <- zuora.request(SubscriptionDetailsViaAmend(latestSubscription.id, latestSubscription.contractAcceptanceDate))
        subscriptionDetails <- subscriptionDetailsF
      } yield {
        logger.info(s"result from amend query: $result")
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

    val summaryViaSubscriptionAmendF = getSummaryViaSubscriptionAmend(memberId)

    for {
      userInvoiced <- hasUserBeenInvoiced(memberId)
      summary <- if (userInvoiced) summaryViaSubscriptionAmendF else getSummaryViaInvoice(memberId)
    } yield summary
  }

  def getSubscriptionsByCasId(casId: String): Future[Seq[Subscription]] =  zuora.query[Subscription](s"CASSubscriberID__c='$casId'")
}
