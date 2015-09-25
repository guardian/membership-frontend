package controllers

import com.github.nscala_time.time.Imports._
import configuration.CopyConfig
import model.RichEvent.CalendarMonthDayGroup
import model._
import play.api.mvc.Controller
import services._
import tracking.ActivityTracking

trait WhatsOn extends Controller with ActivityTracking {

  val guLiveEvents: EventbriteService
  val localEvents: EventbriteService
  val masterclassEvents: EventbriteService

  private def collectAllEvents = {
    guLiveEvents.events ++ localEvents.events ++ masterclassEvents.events
  }

  def calendarGrid = GoogleAuthenticatedStaffAction { implicit request =>
    val pageInfo = PageInfo(
      CopyConfig.copyTitleEvents,
      request.path,
      Some(CopyConfig.copyDescriptionEvents)
    )
    val eventsGroupedByMonth = RichEvent.groupEventsByMonth(collectAllEvents)
    Ok(views.html.whatson.calendarGrid(eventsGroupedByMonth, pageInfo))
  }

  def calendarList = GoogleAuthenticatedStaffAction { implicit request =>
    val pageInfo = PageInfo(
      CopyConfig.copyTitleEvents,
      request.path,
      Some(CopyConfig.copyDescriptionEvents)
    )
    val events = CalendarMonthDayGroup("Calendar", RichEvent.groupEventsByDayAndMonth(collectAllEvents))
    Ok(views.html.whatson.calendarList(pageInfo, events))
  }

}

object WhatsOn extends WhatsOn {
  val guLiveEvents = GuardianLiveEventService
  val localEvents = LocalEventService
  val masterclassEvents = MasterclassEventService
}
