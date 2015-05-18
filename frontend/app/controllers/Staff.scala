package controllers

import admin.AdminForms._
import com.github.nscala_time.time.Imports._
import model.{EventMetadata, EventsByStatus, GroupedEvents}
import play.api.mvc.Controller
import services.{GuardianLiveEventService, LocalEventService, MasterclassEventService, _}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait Staff extends Controller {
  val guLiveEvents = GuardianLiveEventService
  val localEvents = LocalEventService
  val masterclassEvents = MasterclassEventService

  // copied from Event controller, possible to reuse there?
  private def chronologicalSort(events: Seq[model.RichEvent.RichEvent]) = {
    events.sortWith(_.event.start < _.event.start)
  }

  private def getAllEvents: GroupedEvents = {
    val guLivePastEvents = guLiveEvents.getEventsArchive.headOption.map(chronologicalSort(_).reverse)
    val localPastEvents = localEvents.getEventsArchive.headOption.map(chronologicalSort(_).reverse)
    val masterclassPastEvents = masterclassEvents.getEventsArchive.headOption.map(chronologicalSort(_).reverse)

    GroupedEvents(
      EventsByStatus(guLiveEvents.events, guLiveEvents.eventsDraft, guLivePastEvents.get),
      EventsByStatus(localEvents.events, localEvents.eventsDraft, localPastEvents.get),
      EventsByStatus(masterclassEvents.events, masterclassEvents.eventsDraft, masterclassPastEvents.get)
    )
  }

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
    Ok(views.html.staff.admin.adminTool(None, getAllEvents))
  }

  def adminEdit(id: String) = GoogleAuthenticatedStaffAction { implicit request =>
    val event = EventbriteService.getEventAndDraft(id)
    Ok(views.html.staff.admin.adminTool(event, getAllEvents))
  }

  def adminUpdate(id: String) = NoCacheAction.async { implicit request =>
    def update(formData: EditForm) = {
      val event = EventbriteService.getEventAndDraft(id)
      EventMetadataService.create(new EventMetadata(
        ticketingProvider = formData.ticketingProvider,
        ticketingProviderId = id,
        gridUrl = formData.gridUrl
      )) map { response =>
        Ok(views.html.staff.admin.adminTool(event, getAllEvents))
      } recover { case _ => BadRequest }
    }

    editForm.bindFromRequest.fold(_ => Future.successful(BadRequest), update)
  }
}

object Staff extends Staff
