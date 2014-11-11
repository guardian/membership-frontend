package controllers

import play.api.mvc.{Action, Controller}
import services.{MasterclassEventService, GuardianLiveEventService, MasterclassDataService}
import com.gu.monitoring.CloudWatchHealth

object Healthcheck extends Controller {

  def healthcheck() = Action {
    Cached(1)(if (GuardianLiveEventService.events.nonEmpty &&
                  MasterclassEventService.events.nonEmpty &&
                  CloudWatchHealth.hasPushedMetricSuccessfully) Ok("OK")
    else ServiceUnavailable("Service Unavailable"))
  }

}