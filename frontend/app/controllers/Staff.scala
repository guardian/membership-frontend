package controllers

import admin.AdminForms._
import model.EventMetadata
import play.api.mvc.Controller
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import services._

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

  def admin = GoogleAuthenticatedStaffAction { implicit request =>
    Ok(views.html.staff.admin.adminTool(None))
  }

  def adminEdit(id: String) = GoogleAuthenticatedStaffAction { implicit request =>
    val event = EventbriteService.getEvent(id)
    Ok(views.html.staff.admin.adminTool(event))
  }

  def adminUpdate(id: String) = NoCacheAction.async { implicit request =>
    def update(formData: EditForm) = {
      val event = EventbriteService.getEvent(id)
      EventMetadataService.create(new EventMetadata(
        ticketingProvider = formData.ticketingProvider,
        ticketingProviderId = id,
        gridUrl = formData.gridUrl
      )) map { response =>
        Ok(views.html.staff.admin.adminTool(event))
      } recover { case _ => BadRequest }
    }

    editForm.bindFromRequest.fold(_ => Future.successful(BadRequest), update)

  }
}

object Staff extends Staff
