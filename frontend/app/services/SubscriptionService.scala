package services

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import org.joda.time.DateTime

import com.gu.membership.salesforce.{MemberId, Tier}

import configuration.Config
import forms.MemberForm.{NameForm, AddressForm}
import model.Stripe
import model.Subscription._
import model.Zuora._
import model.ZuoraDeserializer._
import utils.ScheduledTask

case class SubscriptionServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}

object SubscriptionServiceHelpers {
  def sortAmendments(subscriptions: Seq[Map[String, String]], amendments: Seq[Map[String, String]]) = {
    val versions = subscriptions.map { amendment => (amendment("Id"), amendment("Version").toInt) }.toMap
    amendments.sortBy { amendment => versions(amendment("SubscriptionId")) }
  }

  def sortSubscriptions(subscriptions: Seq[Map[String, String]]) = subscriptions.sortBy(_("Version").toInt)
}

trait CreateSubscription {
  self: SubscriptionService =>

  def createPaidSubscription(memberId: MemberId, customer: Stripe.Customer, tier: Tier.Tier,
                             annual: Boolean, name: NameForm, address: AddressForm): Future[Subscription] = {
    val plan = PaidPlan(tier, annual)
    zuora.Subscribe(memberId.account, memberId.contact, Some(customer), plan, name, address).mkRequest().map { result =>
      Subscription(result.id)
    }
  }

  def createFriendSubscription(memberId: MemberId, name: NameForm, address: AddressForm): Future[Subscription] = {
    zuora.Subscribe(memberId.account, memberId.contact, None, friendPlan, name, address).mkRequest().map { result =>
      Subscription(result.id)
    }
  }
}

trait AmendSubscription {
  self: SubscriptionService =>

  def cancelSubscription(sfAccountId: String, instant: Boolean): Future[Amendment] = {
    for {
      subscriptionId <- getCurrentSubscriptionId(sfAccountId)
      subscriptionDetails <- getSubscriptionDetails(subscriptionId)

      cancelDate = if (instant) DateTime.now else subscriptionDetails.endDate
      result <- zuora.CancelPlan(subscriptionId, subscriptionDetails.ratePlanId, cancelDate).mkRequest()
    } yield Amendment(result.ids)
  }

  def downgradeSubscription(sfAccountId: String, tier: Tier.Tier, annual: Boolean): Future[Amendment] = {
    val newRatePlanId = tier match {
      case Tier.Friend => friendPlan
      case t => PaidPlan(t, annual)
    }

    for {
      subscriptionId <- getCurrentSubscriptionId(sfAccountId)
      subscriptionDetails <- getSubscriptionDetails(subscriptionId)
      result <- zuora.DowngradePlan(subscriptionId, subscriptionDetails.ratePlanId, newRatePlanId,
        subscriptionDetails.endDate).mkRequest()
    } yield Amendment(result.ids)
  }

  def upgradeSubscription(sfAccountId: String, tier: Tier.Tier, annual: Boolean): Future[Amendment] = {
    val newRatePlanId = PaidPlan(tier, annual)

    for {
      subscriptionId <- getCurrentSubscriptionId(sfAccountId)
      ratePlanId  <- zuora.queryOne("Id", "RatePlan", s"SubscriptionId='$subscriptionId'")
      result <- zuora.UpgradePlan(subscriptionId, ratePlanId, newRatePlanId).mkRequest()
    } yield Amendment(result.ids)
  }
}

trait SubscriptionService extends CreateSubscription with AmendSubscription {
  import SubscriptionServiceHelpers._

  val zuora: ZuoraService

  val friendPlan = Config.zuoraApiFriend

  case class PaidPlan(monthly: String, annual: String)

  object PaidPlan {
    val plans = Map(
      Tier.Partner -> PaidPlan(Config.zuoraApiPartnerMonthly, Config.zuoraApiPartnerAnnual),
      Tier.Patron -> PaidPlan(Config.zuoraApiPatronMonthly, Config.zuoraApiPatronAnnual)
    )

    def apply(tier: Tier.Tier, annual: Boolean): String = {
      val plan = plans(tier)
      if (annual) plan.annual else plan.monthly
    }
  }
  def getAccountId(sfAccountId: String): Future[String] = zuora.queryOne("Id", "Account", s"crmId='$sfAccountId'")

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
      paymentMethod <- zuora.CreatePaymentMethod(accountId, customer).mkRequest()
      result <- zuora.SetDefaultPaymentMethod(accountId, paymentMethod.id).mkRequest()
    } yield accountId
  }

}

object SubscriptionService extends SubscriptionService with ScheduledTask[Authentication] {
  val initialValue = Authentication("", "")
  val initialDelay = 0.seconds
  val interval = 2.hours

  def refresh() = zuora.Login().mkRequest()

  val zuora = new ZuoraService {
    val apiUrl = Config.zuoraApiUrl
    val apiUsername = Config.zuoraApiUsername
    val apiPassword = Config.zuoraApiPassword

    def authentication: Authentication = agent.get()
  }
}
