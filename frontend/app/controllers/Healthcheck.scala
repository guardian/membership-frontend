package controllers

import play.api.Logger
import play.api.mvc.{Action, Controller}
import services.{TouchpointBackend, GuardianLiveEventService}
import com.gu.monitoring.CloudWatchHealth
import com.github.nscala_time.time.Imports._

case class Test(name: String, result: () => Boolean)

object Healthcheck extends Controller {

  val tests = Seq(
    Test("Events", GuardianLiveEventService.events.nonEmpty _),
    Test("CloudWatch", CloudWatchHealth.hasPushedMetricSuccessfully _),
    Test("Zuora", () => TouchpointBackend.Normal.zuoraService.pingTask.get() > DateTime.now - 2.minutes)
  )

  def healthcheck() = Action {
    Cached(1) {
      val serviceOk = tests.forall { test =>
        val result = test.result()
        if (!result) Logger.warn(s"${test.name} test failed, health check will fail")
        result
      }

      if (serviceOk) Ok("OK") else ServiceUnavailable("Service Unavailable")
    }
  }

}