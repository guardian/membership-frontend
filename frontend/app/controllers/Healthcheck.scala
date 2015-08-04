package controllers

import play.api.Logger
import play.api.mvc.{Action, Controller}
import services.{TouchpointBackend, GuardianLiveEventService}
import com.gu.monitoring.CloudWatchHealth
import com.github.nscala_time.time.Imports._
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

case class Test(name: String, result: () => Future[Boolean])

object Healthcheck extends Controller {
  val zuoraService = TouchpointBackend.Normal.zuoraService

  val tests = Seq(
    Test("Events", () => Future { GuardianLiveEventService.events.nonEmpty }),
    Test("CloudWatch", () => Future { CloudWatchHealth.hasPushedMetricSuccessfully }),
    Test("Zuora", () => zuoraService.pingSupplier.get().map(t => t > DateTime.now - 2.minutes))
  )

  def healthcheck() = Action.async { req =>
    Future.sequence(tests.map(_.result())).map { results =>
      val failedTests =
        results.zip(tests.map(_.name)).filterNot { case (ok, _) => ok }

      Cached(1) {
        if (failedTests.nonEmpty) {
          failedTests.foreach { case (_, test) =>
            Logger.warn(s"Test $test failed, health check will fail")
          }
          ServiceUnavailable("Service Unavailable")
        } else {
          Ok("OK")
        }
      }
    }
  }
}
