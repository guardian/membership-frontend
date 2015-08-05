package controllers

import com.github.nscala_time.time.Imports._
import com.gu.monitoring.CloudWatchHealth
import play.api.Logger
import play.api.mvc.{Action, Controller}
import services.{GuardianLiveEventService, TouchpointBackend}

case class Test(name: String, ok: () => Boolean)

object Healthcheck extends Controller {
  val zuoraService = TouchpointBackend.Normal.zuoraService

  val tests = Seq(
    Test("Events", () => GuardianLiveEventService.events.nonEmpty),
    Test("CloudWatch", () => CloudWatchHealth.hasPushedMetricSuccessfully),
    Test("Zuora", () => zuoraService.lastPingTimeWithin(2.minutes))
  )

  def healthcheck() = Action { req =>
    val failedTests = tests.filterNot(_.ok())

    Cached(1) {
      if (failedTests.nonEmpty) {
        failedTests.foreach { test =>
          Logger.warn(s"Test ${test.name} failed, health check will fail")
        }
        ServiceUnavailable("Service Unavailable")
      } else {
        Ok("OK")
      }
    }
  }
}
