package controllers

import play.api.mvc.Controller
import services.{GuardianLiveEventService, EventbriteService}

trait PatternLibrary extends Controller {
  val guLiveEvents: EventbriteService

  def patterns = NoCacheAction { implicit request =>
    Ok(views.html.patterns.patterns(guLiveEvents.getEventPortfolio))
  }

}

object PatternLibrary extends PatternLibrary {
  val guLiveEvents = GuardianLiveEventService
}
