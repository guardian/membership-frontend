package services

import services.zuora._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import com.github.nscala_time.time.Imports._

import com.gu.membership.salesforce.{MemberId, Tier}

import configuration.Config
import forms.MemberForm.JoinForm
import model.Stripe
import model.Subscription.TierPlan
import model.Zuora._
import model.ZuoraDeserializer._

case class SubscriptionServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}

object SubscriptionServiceHelpers {
  def sortAmendments(subscriptions: Seq[Subscription], amendments: Seq[Amendment]) = {
    val versions = subscriptions.map { amendment => (amendment.id, amendment.version) }.toMap
    amendments.sortBy { amendment => versions(amendment.subscriptionId) }
  }

  def sortSubscriptions(subscriptions: Seq[Subscription]) = subscriptions.sortBy(_.version)

  def sortAccounts(accounts: Seq[Account]) = accounts.sortBy(_.createdDate)
}

trait AmendSubscription {
  self: SubscriptionService =>

  private def checkForPendingAmendments(sfAccountId: String)(fn: String => Future[AmendResult]): Future[AmendResult] = {
    getSubscriptionStatus(sfAccountId).flatMap { subscriptionStatus =>
      if (subscriptionStatus.future.isEmpty) {
        fn(subscriptionStatus.current)
      } else {
        throw SubscriptionServiceError("Cannot amend subscription, amendments are already pending")
      }
    }
  }

  def cancelSubscription(sfAccountId: String, instant: Boolean): Future[AmendResult] = {
    checkForPendingAmendments(sfAccountId) { subscriptionId =>
      for {
        subscriptionDetails <- getSubscriptionDetails(subscriptionId)
        cancelDate = if (instant) DateTime.now else subscriptionDetails.endDate
        result <- zuora.request(CancelPlan(subscriptionId, subscriptionDetails.ratePlanId, cancelDate))
      } yield result
    }
  }

  def downgradeSubscription(sfAccountId: String, newTierPlan: TierPlan): Future[AmendResult] = {
    checkForPendingAmendments(sfAccountId) { subscriptionId =>
      for {
        subscriptionDetails <- getSubscriptionDetails(subscriptionId)
        result <- zuora.request(DowngradePlan(subscriptionId, subscriptionDetails.ratePlanId,
          tierPlanRateIds(newTierPlan), subscriptionDetails.endDate))
      } yield result
    }
  }

  def upgradeSubscription(sfAccountId: String, newTierPlan: TierPlan): Future[AmendResult] = {
    checkForPendingAmendments(sfAccountId) { subscriptionId =>
      for {
        ratePlan <- zuora.queryOne[RatePlan](s"SubscriptionId='$subscriptionId'")
        result <- zuora.request(UpgradePlan(subscriptionId, ratePlan.id, tierPlanRateIds(newTierPlan)))
      } yield result
    }
  }
}

class SubscriptionService(val tierPlanRateIds: Map[TierPlan, String], val zuora: ZuoraService) extends AmendSubscription {
  import SubscriptionServiceHelpers._

  def getAccount(sfAccountId: String): Future[Account] =
    zuora.query[Account](s"crmId='$sfAccountId'").map(sortAccounts(_).last)

  def getSubscriptionStatus(sfAccountId: String): Future[SubscriptionStatus] = {
    for {
      account <- getAccount(sfAccountId)
      subscriptions <- zuora.query[Subscription](s"AccountId='${account.id}'")

      if subscriptions.size > 0

      where = subscriptions.map { sub => s"SubscriptionId='${sub.id}'" }.mkString(" OR ")
      amendments <- zuora.query[Amendment](where)
    } yield {
      val latestSubscriptionId = sortSubscriptions(subscriptions).last.id

      sortAmendments(subscriptions, amendments)
        .find(_.contractEffectiveDate.isAfterNow)
        .fold(SubscriptionStatus(latestSubscriptionId, None)) { amendment =>
          SubscriptionStatus(amendment.subscriptionId, Some(latestSubscriptionId))
        }
    }
  }

  def getSubscriptionDetails(subscriptionId: String): Future[SubscriptionDetails] = {
    for {
      ratePlan <- zuora.queryOne[RatePlan](s"SubscriptionId='$subscriptionId'")
      ratePlanCharge <- zuora.queryOne[RatePlanCharge](s"RatePlanId='${ratePlan.id}'")
    } yield SubscriptionDetails(ratePlan, ratePlanCharge)
  }

  def getCurrentSubscriptionId(sfAccountId: String): Future[String] = getSubscriptionStatus(sfAccountId).map(_.current)

  def getCurrentSubscriptionDetails(sfAccountId: String): Future[SubscriptionDetails] = {
    for {
      subscriptionId <- getCurrentSubscriptionId(sfAccountId)
      subscriptionDetails <- getSubscriptionDetails(subscriptionId)
    } yield subscriptionDetails
  }

  def createPaymentMethod(sfAccountId: String, customer: Stripe.Customer): Future[UpdateResult] = {
    for {
      account <- getAccount(sfAccountId)
      paymentMethod <- zuora.request(CreatePaymentMethod(account.id, customer))
      result <- zuora.request(SetDefaultPaymentMethod(account.id, paymentMethod.id))
    } yield result
  }

  def createSubscription(memberId: MemberId, joinData: JoinForm, customerOpt: Option[Stripe.Customer]): Future[SubscribeResult] = {
    zuora.request(Subscribe(memberId.account, memberId.contact, customerOpt, tierPlanRateIds(joinData.tierPlan),
      joinData.name, joinData.deliveryAddress))
  }
}
