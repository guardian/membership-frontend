package controllers

import play.api.Logger
import play.api.mvc.{Action, Controller}
import services.{TouchpointBackend, GuardianLiveEventService}
import com.gu.monitoring.CloudWatchHealth
import com.github.nscala_time.time.Imports._

object Healthcheck extends Controller {

  val tests = Map(
    "Events" -> GuardianLiveEventService.events.nonEmpty _,
    "CloudWatch" -> CloudWatchHealth.hasPushedMetricSuccessfully _,
    "Zuora" -> (() => {
      val lastPing = TouchpointBackend.Normal.zuoraService.pingTask.get()
      val delta = (lastPing to DateTime.now).duration
      if (delta > 2.minutes) {
        Logger.error(s"Zuora has not responded for ${delta.toStandardSeconds.getSeconds} seconds")
        false
      } else {
        true
      }
    })
  )

  def healthcheck() = Action {
    Cached(1) {
      val zuoraLastPing = TouchpointBackend.Normal.zuoraService.pingTask.get()

      val serviceOk = tests.forall { case (name, fn) =>
          val result = fn()
          if (!result) {
            Logger.error(s"$name test failed, health check will fail")
          }
          result
      }

      if (serviceOk) Ok("OK")
      else ServiceUnavailable("Service Unavailable")
    }
  }

}