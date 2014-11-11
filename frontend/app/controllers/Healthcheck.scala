package controllers

import play.api.mvc.{Action, Controller}
import services.{GuardianLiveEventService, MasterclassDataService}
import com.gu.monitoring.CloudWatchHealth

object Healthcheck extends Controller {

  def healthcheck() = Action {
    Cached(1)(if (GuardianLiveEventService.events.nonEmpty &&
                  MasterclassDataService.masterclassData.nonEmpty &&
                  CloudWatchHealth.hasPushedMetricSuccessfully) Ok("OK")
    else ServiceUnavailable("Service Unavailable"))
  }

}