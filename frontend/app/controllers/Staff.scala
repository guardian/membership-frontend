package controllers

import play.api.mvc.Controller
import services.{GuardianLiveEventService, MasterclassEventService}

trait Staff extends Controller {
  val guLiveEvents = GuardianLiveEventService
  val masterclassEvents = MasterclassEventService

  def eventOverview = GoogleAuthenticatedStaffAction { implicit request =>
     Ok(views.html.staff.eventOverview.guLive(guLiveEvents.events, guLiveEvents.eventsDraft))
  }

  def masterclassOverview = GoogleAuthenticatedStaffAction { implicit request =>
     Ok(views.html.staff.eventOverview.masterclass(masterclassEvents.events, masterclassEvents.eventsDraft))
  }
}

object Staff extends Staff
