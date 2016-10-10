package controllers

import com.gu.memsub.services.CatalogService
import configuration.Config.Implicits.akkaSystem
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.Controller
import services._


trait Staff extends Controller {
  val guLiveEvents = GuardianLiveEventService
  val localEvents = LocalEventService
  val masterclassEvents = MasterclassEventService

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
