package controllers

import play.api.mvc.Controller
import services.{LocalEventService, GuardianLiveEventService, MasterclassEventService}

trait Staff extends Controller {
  val guLiveEvents = GuardianLiveEventService
  val localEvents = LocalEventService
  val masterclassEvents = MasterclassEventService

  def eventOverview = GoogleAuthenticatedStaffAction { implicit request =>
     Ok(views.html.staff.eventOverview.guLive(guLiveEvents.events, guLiveEvents.eventsDraft, request.path))
  }

  def localOverview = GoogleAuthenticatedStaffAction { implicit request =>
     Ok(views.html.staff.eventOverview.local(localEvents.events, localEvents.eventsDraft, request.path))
  }

  def masterclassOverview = GoogleAuthenticatedStaffAction { implicit request =>
     Ok(views.html.staff.eventOverview.masterclass(masterclassEvents.events, masterclassEvents.eventsDraft, request.path))
  }

  def eventDetails = GoogleAuthenticatedStaffAction { implicit request =>
    Ok(views.html.staff.event.details(request.path))
  }
}

object Staff extends Staff
