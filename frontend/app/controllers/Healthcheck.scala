package controllers

import play.api.mvc.{Action, Controller}
import services.EventbriteService

object Healthcheck extends Controller {

  def healthcheck() = Action {
    Cached(1)(if (EventbriteService.events.nonEmpty) Ok("OK") else ServiceUnavailable("Service Unavailable"))
  }

}