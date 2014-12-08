package controllers

import play.api.mvc.Controller
import services.{GuardianLiveEventService, EventbriteService}

trait Styleguide extends Controller {
  val guLiveEvents: EventbriteService

  def patterns = NoCacheAction { implicit request =>
    Ok(views.html.styleguide.patterns(guLiveEvents.getEventPortfolio))
  }

}

object Styleguide extends Styleguide {
  val guLiveEvents = GuardianLiveEventService
}
