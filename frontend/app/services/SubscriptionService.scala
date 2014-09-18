package services

import services.zuora._

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import com.github.nscala_time.time.Imports._

import com.gu.membership.salesforce.{MemberId, Tier}

import configuration.Config
import forms.MemberForm.{JoinForm, NameForm, AddressForm}
import model.Stripe
import model.Subscription._
import model.Zuora._
import model.ZuoraDeserializer._

case class SubscriptionServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}

object SubscriptionServiceHelpers {
  def sortAmendments(subscriptions: Seq[Map[String, String]], amendments: Seq[Map[String, String]]) = {
    val versions = subscriptions.map { amendment => (amendment("Id"), amendment("Version").toInt) }.toMap
    amendments.sortBy { amendment => versions(amendment("SubscriptionId")) }
  }

  def sortSubscriptions(subscriptions: Seq[Map[String, String]]) = subscriptions.sortBy(_("Version").toInt)

  def sortAccounts(accounts: Seq[Map[String, String]]) = accounts.sortBy { account =>
    new DateTime(account("CreatedDate"))
  }
}

trait AmendSubscription {
  self: SubscriptionService =>

  private def checkForPendingAmendments(sfAccountId: String)(fn: String => Future[Amendment]): Future[Amendment] = {
    getSubscriptionStatus(sfAccountId).flatMap { subscriptionStatus =>
      if (subscriptionStatus.future.isEmpty) {
        fn(subscriptionStatus.current)
      } else {
        throw SubscriptionServiceError("Cannot amend subscription, amendments are already pending")
      }
    }
  }

  def cancelSubscription(sfAccountId: String, instant: Boolean): Future[Amendment] = {
    checkForPendingAmendments(sfAccountId) { subscriptionId =>
      for {
        subscriptionDetails <- getSubscriptionDetails(subscriptionId)
        cancelDate = if (instant) DateTime.now else subscriptionDetails.endDate
        result <- zuora.mkRequest(CancelPlan(subscriptionId, subscriptionDetails.ratePlanId, cancelDate))
      } yield Amendment(result.ids)
    }
  }

  def downgradeSubscription(sfAccountId: String, newTierPlan: TierPlan): Future[Amendment] = {
    checkForPendingAmendments(sfAccountId) { subscriptionId =>
      for {
        subscriptionDetails <- getSubscriptionDetails(subscriptionId)
        result <- zuora.mkRequest(DowngradePlan(subscriptionId, subscriptionDetails.ratePlanId,
          tierPlanRateIds(newTierPlan), subscriptionDetails.endDate))
      } yield Amendment(result.ids)
    }
  }

  def upgradeSubscription(sfAccountId: String, newTierPlan: TierPlan): Future[Amendment] = {
    checkForPendingAmendments(sfAccountId) { subscriptionId =>
      for {
        ratePlanId <- zuora.queryOne("Id", "RatePlan", s"SubscriptionId='$subscriptionId'")
        result <- zuora.mkRequest(UpgradePlan(subscriptionId, ratePlanId, tierPlanRateIds(newTierPlan)))
      } yield Amendment(result.ids)
    }
  }
}

class SubscriptionService(val tierPlanRateIds: Map[TierPlan, String], val zuora: ZuoraService) extends AmendSubscription {
  import SubscriptionServiceHelpers._

  def getAccountId(sfAccountId: String): Future[String] =
    zuora.query(Seq("Id", "CreatedDate"), "Account", s"crmId='$sfAccountId'").map(sortAccounts(_).last("Id"))

  def getSubscriptionStatus(sfAccountId: String): Future[SubscriptionStatus] = {
    for {
      accountId <- getAccountId(sfAccountId)

      subscriptions <- zuora.query(Seq("Id", "Version"), "Subscription", s"AccountId='$accountId'")

      if subscriptions.size > 0

      where = subscriptions.map { sub => s"SubscriptionId='${sub("Id")}'" }.mkString(" OR ")
      amendments <- zuora.query(Seq("ContractEffectiveDate", "SubscriptionId"), "Amendment", where)
    } yield {
      val latestSubscriptionId = sortSubscriptions(subscriptions).last("Id")

      sortAmendments(subscriptions, amendments)
        .find { amendment => new DateTime(amendment("ContractEffectiveDate")).isAfterNow }
        .fold(SubscriptionStatus(latestSubscriptionId, None)) { amendment =>
          SubscriptionStatus(amendment("SubscriptionId"), Some(latestSubscriptionId))
        }
    }
  }

  def getSubscriptionDetails(subscriptionId: String): Future[SubscriptionDetails] = {
    for {
      ratePlan <- zuora.queryOne(Seq("Id", "Name"), "RatePlan", s"SubscriptionId='$subscriptionId'")
      ratePlanCharge <- zuora.queryOne(Seq("ChargedThroughDate", "EffectiveStartDate", "Price"), "RatePlanCharge", s"RatePlanId='${ratePlan("Id")}'")
    } yield SubscriptionDetails(ratePlan, ratePlanCharge)
  }

  def getCurrentSubscriptionId(sfAccountId: String): Future[String] = getSubscriptionStatus(sfAccountId).map(_.current)

  def getCurrentSubscriptionDetails(sfAccountId: String): Future[SubscriptionDetails] = {
    for {
      subscriptionId <- getCurrentSubscriptionId(sfAccountId)
      subscriptionDetails <- getSubscriptionDetails(subscriptionId)
    } yield subscriptionDetails
  }

  def createPaymentMethod(sfAccountId: String, customer: Stripe.Customer): Future[String] = {
    for {
      accountId <- getAccountId(sfAccountId)
      paymentMethod <- zuora.mkRequest(CreatePaymentMethod(accountId, customer))
      result <- zuora.mkRequest(SetDefaultPaymentMethod(accountId, paymentMethod.id))
    } yield accountId
  }

  def createSubscription(memberId: MemberId, joinData: JoinForm, customerOpt: Option[Stripe.Customer]): Future[Subscription] = {
    zuora.mkRequest(Subscribe(memberId.account, memberId.contact, customerOpt, tierPlanRateIds(joinData.tierPlan), joinData.name,
      joinData.deliveryAddress)).map { result =>
      Subscription(result.id)
    }
  }
}
