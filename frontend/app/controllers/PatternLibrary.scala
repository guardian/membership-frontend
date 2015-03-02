package controllers

import model.EventPortfolio
import play.api.mvc.Controller
import services.{GuardianLiveEventService, EventbriteService}

trait PatternLibrary extends Controller {
  val guLiveEvents: EventbriteService

  def patterns = NoCacheAction { implicit request =>
    Ok(views.html.patterns.patterns(
      EventPortfolio(
        guLiveEvents.getFeaturedEvents,
        guLiveEvents.getEvents,
        guLiveEvents.getEventsArchive,
        guLiveEvents.getPartnerEvents
      )))
  }

}

object PatternLibrary extends PatternLibrary {
  val guLiveEvents = GuardianLiveEventService
}
