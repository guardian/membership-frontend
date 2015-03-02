package controllers

import play.api.mvc.Controller
import services.{DiscoverEventService, GuardianLiveEventService, MasterclassEventService}

trait Staff extends Controller {
  val guLiveEvents = GuardianLiveEventService
  val discoverEvents = DiscoverEventService
  val masterclassEvents = MasterclassEventService

  def eventOverview = GoogleAuthenticatedStaffAction { implicit request =>
     Ok(views.html.staff.eventOverview.guLive(guLiveEvents.events, guLiveEvents.eventsDraft, request.path))
  }

  def discoverOverview = GoogleAuthenticatedStaffAction { implicit request =>
     Ok(views.html.staff.eventOverview.discover(discoverEvents.events, discoverEvents.eventsDraft, request.path))
  }

  def masterclassOverview = GoogleAuthenticatedStaffAction { implicit request =>
     Ok(views.html.staff.eventOverview.masterclass(masterclassEvents.events, masterclassEvents.eventsDraft, request.path))
  }

  def eventDetails = GoogleAuthenticatedStaffAction { implicit request =>
    Ok(views.html.staff.event.details(request.path))
  }
}

object Staff extends Staff
