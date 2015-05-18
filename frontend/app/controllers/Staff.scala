package controllers

import model.EventsByStatus
import play.api.mvc.Controller
import services.{LocalEventService, GuardianLiveEventService, MasterclassEventService}
import com.github.nscala_time.time.Imports._

trait Staff extends Controller {
  val guLiveEvents = GuardianLiveEventService
  val localEvents = LocalEventService
  val masterclassEvents = MasterclassEventService

  // copied from Event controller, possible to reuse there?
  private def chronologicalSort(events: Seq[model.RichEvent.RichEvent]) = {
    events.sortWith(_.event.start < _.event.start)
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
    val guLivePastEvents = guLiveEvents.getEventsArchive.headOption.map(chronologicalSort(_).reverse)
    val localPastEvents = localEvents.getEventsArchive.headOption.map(chronologicalSort(_).reverse)
    val masterclassPastEvents = masterclassEvents.getEventsArchive.headOption.map(chronologicalSort(_).reverse)

    val guLive = EventsByStatus(guLiveEvents.events, guLiveEvents.eventsDraft, guLivePastEvents.get)
    val local = EventsByStatus(localEvents.events, localEvents.eventsDraft, localPastEvents.get)
    val masterclasses = EventsByStatus(masterclassEvents.events, masterclassEvents.eventsDraft, masterclassPastEvents.get)

    Ok(views.html.staff.admin.adminTool(guLive, local, masterclasses))
  }
}

object Staff extends Staff
