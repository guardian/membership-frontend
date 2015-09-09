package controllers

import com.gu.membership.model.{FriendTierPlan, PaidTierPlan, StaffPlan}
import com.gu.membership.salesforce.Tier.{Partner, Patron, Supporter}
import com.gu.monitoring.CloudWatchHealth
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.{Action, Controller}
import services.{GuardianLiveEventService, TouchpointBackend}

import scala.concurrent.Await
import scala.util.{Try, Failure, Success}

case class Test(name: String, exec: () => Boolean) {
  def failedTest() = Logger.warn(s"Test $name failed, health check will fail")

  def ok() = {
    val passed = exec()
    if (!passed) Logger.warn(s"Test $name failed, health check will fail")
    passed
  }
}

object ZuoraPing {
  import com.github.nscala_time.time.Imports._
  val zuoraSoapService = TouchpointBackend.Normal.zuoraSoapService
  def ping = zuoraSoapService.lastPingTimeWithin(2.minutes)
}

object Healthcheck extends Controller {
  import scala.concurrent.duration._

  val zuoraSoapService = TouchpointBackend.Normal.zuoraSoapService
  val subscriptionService = TouchpointBackend.Normal.subscriptionService

  val productRatePlanTiers = List(FriendTierPlan, StaffPlan,
    PaidTierPlan(Supporter, true), PaidTierPlan(Supporter, false),
    PaidTierPlan(Partner, true), PaidTierPlan(Partner, false),
    PaidTierPlan(Patron, true), PaidTierPlan(Patron, false))

  def tests = Seq(
    Test("Events", () => GuardianLiveEventService.events.nonEmpty),
    Test("CloudWatch", () => CloudWatchHealth.hasPushedMetricSuccessfully),
    Test("ZuoraPing", () => ZuoraPing.ping)) ++
    productRatePlanTiers.map(tier => Test(s"ZuoraCatalog: $tier", () =>
      Try(Await.result(subscriptionService.tierRatePlanId(tier).map(_ => true), 10 millis)) match {
        case Success(passed) => passed
        case Failure(e) => false
      }))

  def healthcheck() = Action {
    Cached(1) {
      val healthy = tests.forall(_.ok())
      if (healthy) Ok("OK")
      else ServiceUnavailable("Service Unavailable")
    }
  }
}
