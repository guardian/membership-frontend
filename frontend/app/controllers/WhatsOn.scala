package controllers

import com.github.nscala_time.time.Imports._
import configuration.CopyConfig
import model.RichEvent
import model.RichEvent.MasterclassEvent._
import model.RichEvent._
import model._
import play.api.mvc.Controller
import services._
import services.eventbrite.{GuardianLiveEventService, MasterclassEventService, LocalEventService, EventbriteService}
import tracking.ActivityTracking

trait WhatsOn extends Controller with ActivityTracking {

  val guLiveEvents: EventbriteService
  val localEvents: EventbriteService
  val masterclassEvents: EventbriteService

  private def allEvents = {
    guLiveEvents.events ++ localEvents.events
  }

  private def allEventsByLocation(location: String) = {
    guLiveEvents.getEventsByLocation(location) ++ localEvents.getEventsByLocation(location)
  }

  private def allEventsInArchive = {
    guLiveEvents.getEventsArchive.toList.flatten ++ localEvents.getEventsArchive.toList.flatten
  }

  private def locationFilterItems = {
    getCitiesWithCount(allEvents).map(FilterItem.tupled)
  }

  def list = CachedAction { implicit request =>
    val pageInfo = PageInfo(
      CopyConfig.copyTitleEvents,
      request.path,
      Some(CopyConfig.copyDescriptionEvents)
    )

    val locationOpt = request.getQueryString("location").filter(_.trim.nonEmpty)
    val featuredEvents = EventGroup("Featured", guLiveEvents.getFeaturedEvents)
    val events = EventGroup("What's on", chronologicalSort(locationOpt.fold(allEvents)(allEventsByLocation)))

    Ok(views.html.event.eventsList(
      pageInfo,
      events,
      featuredEvents,
      locationFilterItems,
      locationOpt
    ))
  }

  def calendar = CachedAction { implicit request =>
    val locationOpt = request.getQueryString("location").filter(_.trim.nonEmpty)

    val calendarEvents =
      CalendarMonthDayGroup("Calendar", groupEventsByDayAndMonth(locationOpt.fold(allEvents)(allEventsByLocation)))

    Ok(views.html.event.calendar(
      PageInfo(s"${calendarEvents.title} | Events", request.path, None),
      calendarEvents,
      locationFilterItems,
      locationOpt
    ))
  }

  def listArchive = CachedAction { implicit request =>
    val calendarArchive =
      CalendarMonthDayGroup("Archive", groupEventsByDayAndMonth(allEventsInArchive)(implicitly[Ordering[LocalDate]].reverse))

    Ok(views.html.event.eventsListArchive(
      PageInfo(s"${calendarArchive.title} | Events", request.path, None),
      calendarArchive
    ))
  }

  def masterclassesList = CachedAction { implicit request =>
    val pageInfo = PageInfo(
      CopyConfig.copyTitleMasterclasses,
      request.path,
      Some(CopyConfig.copyDescriptionMasterclasses)
    )
    val eventGroup = EventGroup("Masterclasses", masterclassEvents.events)
    Ok(views.html.event.masterclassesList(pageInfo, eventGroup))
  }

  def masterclassesListFilteredBy(rawTag: String, rawSubTag: String = "") = CachedAction { implicit request =>
    val pageInfo = PageInfo(
      CopyConfig.copyTitleMasterclasses,
      request.path,
      Some(CopyConfig.copyDescriptionMasterclasses)
    )
    val tag = decodeTag( if(rawSubTag.nonEmpty) rawSubTag else rawTag )
    val eventGroup = EventGroup("Masterclasses", masterclassEvents.getTaggedEvents(tag))
    Ok(views.html.event.masterclassesList(pageInfo, eventGroup, decodeTag(rawTag), decodeTag(rawSubTag)))
  }

}

object WhatsOn extends WhatsOn {
  val guLiveEvents = GuardianLiveEventService
  val localEvents = LocalEventService
  val masterclassEvents = MasterclassEventService
}
