package controllers

import akka.agent.Agent
import com.gu.membership.model.{FriendTierPlan, PaidTierPlan, StaffPlan}
import com.gu.membership.salesforce.Tier.{Partner, Patron, Supporter}
import com.gu.monitoring.CloudWatchHealth
import play.api.Logger
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import play.api.mvc.{Action, Controller}
import services.{GuardianLiveEventService, TouchpointBackend}
import play.api.Play.current

import scala.concurrent.Future

case class TestCase(name: String, exec: () => Future[Boolean]) {
  def failedTest() = Logger.warn(s"Test $name failed, health check will fail")

  def ok() = {
    val result = exec()
    result.map(passed => if (!passed) failedTest())
    result.onFailure { case _ => failedTest() }
    result
  }
}

object Test {
  def apply(name: String, ok: () => Boolean) = TestCase(name, () => Future(ok()))
}

object ZuoraPing {
  import com.github.nscala_time.time.Imports._
  val zuoraSoapService = TouchpointBackend.Normal.zuoraSoapService
  def ping = zuoraSoapService.lastPingTimeWithin(2.minutes)
}

object Healthcheck extends Controller {
  import scala.concurrent.duration._

  val subscriptionService = TouchpointBackend.Normal.subscriptionService

  private val health = Agent(false)

  val productRatePlanTiers = List(FriendTierPlan, StaffPlan,
    PaidTierPlan(Supporter, true), PaidTierPlan(Supporter, false),
    PaidTierPlan(Partner, true), PaidTierPlan(Partner, false),
    PaidTierPlan(Patron, true), PaidTierPlan(Patron, false))

  val tests:Seq[TestCase] = Seq(
    Test("Events", () => GuardianLiveEventService.events.nonEmpty),
    Test("CloudWatch", () => CloudWatchHealth.hasPushedMetricSuccessfully),
    Test("ZuoraPing", () => ZuoraPing.ping)) ++
    productRatePlanTiers.map(tier => TestCase(s"ZuoraCatalog: $tier", () =>
      subscriptionService.tierPlanRateIds(tier).map(_ => true)))

  def updateHealthCheck() = {
    Future.sequence(tests.map(_.ok()))
      .map { results => health send results.forall(r => r) }
      .recover { case _ => health send false }
  }

  Akka.system.scheduler.schedule(initialDelay = 0.minutes, interval = 1.minute)(updateHealthCheck())

  def healthcheck() = Action {
    Cached(1) {
      updateHealthCheck()
      if (health.get()) Ok("OK")
      else ServiceUnavailable("Service Unavailable")
    }
  }
}
