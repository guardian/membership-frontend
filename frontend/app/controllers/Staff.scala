package controllers

import actions.OAuthActions
import play.api.libs.ws.WSClient
import play.api.mvc.Controller
import services._


class Staff(override val wsClient: WSClient) extends Controller with OAuthActions {
  lazy val guLiveEvents = GuardianLiveEventService
  lazy val masterclassEvents = MasterclassEventService

  def eventOverview = GoogleAuthenticatedStaffAction { implicit request =>
     Ok(views.html.eventOverview.live(guLiveEvents.events, guLiveEvents.eventsDraft, request.path))
  }

  def eventOverviewMasterclasses = GoogleAuthenticatedStaffAction { implicit request =>
     Ok(views.html.eventOverview.masterclasses(masterclassEvents.events, masterclassEvents.eventsDraft, request.path))
  }

  def eventOverviewDetails = GoogleAuthenticatedStaffAction { implicit request =>
    Ok(views.html.eventOverview.details(request.path))
  }
}

