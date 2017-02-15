package controllers

import com.github.nscala_time.time.Imports._
import com.gu.monitoring.CloudWatchHealth
import play.api.Logger.warn
import play.api.mvc.{Action, Controller}
import services.{GuardianLiveEventService, TouchpointBackend}

trait Test {
  def ok: Boolean
  def messages: Seq[String] = Nil
}

class BoolTest(name: String, exec: () => Boolean) extends Test {
  override def messages = List(s"Test $name failed, health check will fail")
  override def ok = exec()
}

object Healthcheck extends Controller {
  import TouchpointBackend.Normal._

  def tests = Seq(
    new BoolTest("Events", () => GuardianLiveEventService.events.nonEmpty),
    new BoolTest("CloudWatch", () => CloudWatchHealth.hasPushedMetricSuccessfully),
    new BoolTest("ZuoraPing", () => zuoraSoapClient.lastPingTimeWithin(2.minutes)),
    new BoolTest("Promotions", () => promos.futureAll.value.exists(_.map(_.nonEmpty).getOrElse(false))),
    new BoolTest("Salesforce", () => salesforceService.isAuthenticated)
  )

  def healthcheck() = Action {
    Cached(1) {
      val failures = tests.filterNot(_.ok)
      if (failures.isEmpty) {
        Ok("OK")
      } else {
        failures.flatMap(_.messages).foreach(msg => warn(msg))
        ServiceUnavailable("Service Unavailable")
      }
    }
  }
}
