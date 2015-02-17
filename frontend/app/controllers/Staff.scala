package controllers

import play.api.mvc.Controller
import services.GuardianLiveEventService

trait Staff extends Controller {
  val guLiveEvents = GuardianLiveEventService

  def eventOverview = GoogleAuthenticatedStaffAction { implicit request =>
     Ok(views.html.staff.eventOverview(guLiveEvents.events, guLiveEvents.eventsDraft))
  }
}

object Staff extends Staff
