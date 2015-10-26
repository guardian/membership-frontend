package controllers

import play.api.mvc.Controller
import services.eventbrite.{GuardianLiveEventCache, MasterclassEventCache, LocalEventCache}

trait Staff extends Controller {
  val guLiveEvents = GuardianLiveEventCache
  val localEvents = LocalEventCache
  val masterclassEvents = MasterclassEventCache

  def eventOverview = GoogleAuthenticatedStaffAction { implicit request =>
     Ok(views.html.eventOverview.live(guLiveEvents.events, guLiveEvents.eventsDraft, request.path))
  }

  def eventOverviewLocal = GoogleAuthenticatedStaffAction { implicit request =>
     Ok(views.html.eventOverview.local(localEvents.events, localEvents.eventsDraft, request.path))
  }

  def eventOverviewMasterclasses = GoogleAuthenticatedStaffAction { implicit request =>
     Ok(views.html.eventOverview.masterclasses(masterclassEvents.events, masterclassEvents.eventsDraft, request.path))
  }

  def eventOverviewDetails = GoogleAuthenticatedStaffAction { implicit request =>
    Ok(views.html.eventOverview.details(request.path))
  }
}

object Staff extends Staff
