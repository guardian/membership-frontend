package controllers

import play.api.mvc.{Action, Controller}
import services.{TouchpointBackend, GuardianLiveEventService}
import com.gu.monitoring.CloudWatchHealth
import com.github.nscala_time.time.Imports._

object Healthcheck extends Controller {

  def healthcheck() = Action {
    Cached(1) {
      val zuoraLastPing = TouchpointBackend.Normal.zuoraService.pingTask.get()

      if (GuardianLiveEventService.events.nonEmpty &&
        CloudWatchHealth.hasPushedMetricSuccessfully &&
        zuoraLastPing > DateTime.now - 2.minutes) Ok("OK")
      else ServiceUnavailable("Service Unavailable")
    }
  }

}