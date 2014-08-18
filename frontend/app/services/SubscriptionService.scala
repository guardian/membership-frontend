package services

import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import akka.agent.Agent

import org.joda.time.DateTime

import com.gu.membership.salesforce.{MemberId, Tier}

import play.api.Logger
import play.api.Play.current
import play.api.libs.concurrent.Akka

import configuration.Config
import forms.MemberForm.{NameForm, AddressForm}
import model.Zuora._
import model.Stripe

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
    zuora.Subscribe(memberId.account, memberId.contact, Some(customer), plan, name, address).mkRequest().map(Subscription(_))
  }

  def createFriendSubscription(memberId: MemberId, name: NameForm, address: AddressForm): Future[Subscription] = {
    zuora.Subscribe(memberId.account, memberId.contact, None, friendPlan, name, address).mkRequest().map(Subscription(_))
  }
}

trait AmendSubscription {
  self: SubscriptionService =>

  def downgradeSubscription(sfAccountId: String, tier: Tier.Tier, annual: Boolean): Future[String] = {
    val newRatePlanId = tier match {
      case Tier.Friend => friendPlan
      case t => PaidPlan(t, annual)
    }

    for {
      subscriptionId <- getCurrentSubscriptionId(sfAccountId)
      subscriptionDetails <- getSubscriptionDetails(subscriptionId)
      result <- zuora.DowngradePlan(subscriptionId, subscriptionDetails.ratePlanId, newRatePlanId,
        subscriptionDetails.endDate).mkRequest()
    } yield ""
  }

  def upgradeSubscription(sfAccountId: String, tier: Tier.Tier, annual: Boolean): Future[Subscription] = {
    val newRatePlanId = PaidPlan(tier, annual)

    for {
      subscriptionId <- getCurrentSubscriptionId(sfAccountId)
      ratePlanId  <- zuora.queryOne("Id", "RatePlan", s"SubscriptionId='$subscriptionId'")
      result <- zuora.UpgradePlan(subscriptionId, ratePlanId, newRatePlanId).mkRequest()
    } yield Subscription(result)
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

      subscriptions <- zuora.Query(s"SELECT Id, Version FROM Subscription WHERE AccountId='$accountId'").mkRequest()
        .map { Query(_).results }

      if subscriptions.size > 0

      where = subscriptions.map { sub => s"SubscriptionId='${sub("Id")}'" }.mkString(" OR ")
      amendments <- zuora.Query(s"SELECT ContractEffectiveDate, SubscriptionId FROM Amendment WHERE $where").mkRequest().map(Query(_).results)
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
    } yield SubscriptionDetails(ratePlan ++ ratePlanCharge)
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
      paymentMethod <- zuora.CreatePaymentMethod(accountId, customer).mkRequest().map(PaymentMethod(_))
      _ <- zuora.SetDefaultPaymentMethod(accountId, paymentMethod.id).mkRequest()
    } yield accountId
  }

}

object SubscriptionService extends SubscriptionService {
  val zuora = new ZuoraService {
    val apiUrl = Config.zuoraApiUrl
    val apiUsername = Config.zuoraApiUsername
    val apiPassword = Config.zuoraApiPassword

    def authentication: Authentication = authenticationAgent.get()
  }

  private implicit val system = Akka.system

  val authenticationAgent = Agent[Authentication](Authentication("", ""))

  def refresh() {
    Logger.debug("Refreshing Zuora login")
    authenticationAgent.sendOff(_ => {
      val auth = Await.result(zuora.Login().mkRequest().map(Authentication(_)), 15.seconds)
      Logger.debug(s"Got Zuora login $auth")
      auth
    })
  }

  def start() {
    Logger.info("Starting Zuora background tasks")
    system.scheduler.schedule(0.seconds, 2.hours) { refresh() }
  }
}
