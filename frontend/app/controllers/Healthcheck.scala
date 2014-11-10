package controllers

import play.api.mvc.{Action, Controller}
import services.{GuardianLiveEventService, MasterclassDataService}

object Healthcheck extends Controller {

  def healthcheck() = Action {
    Cached(1)(if (GuardianLiveEventService.events.nonEmpty && MasterclassDataService.masterclassData.nonEmpty) Ok("OK") else ServiceUnavailable("Service Unavailable"))
  }

}