package controllers

import com.github.nscala_time.time.Imports._
import com.gu.i18n.CountryGroup._
import configuration.CopyConfig
import model.RichEvent.MasterclassEvent._
import model.RichEvent._
import play.api.mvc.Controller
import services._
import tracking.ActivityTracking
import views.support.PageInfo
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

trait WhatsOn extends Controller with ActivityTracking {
  implicit val countryGroup = UK

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

  private val normalCatalog =
    Future.successful(TouchpointBackend.Normal.catalogService.unsafeCatalog)

  def list = CachedAction.async { implicit request =>
    val pageInfo = PageInfo(
      title = CopyConfig.copyTitleEvents,
      url = request.path,
      description = Some(CopyConfig.copyDescriptionEvents)
    )

    val locationOpt = request.getQueryString("location").filter(_.trim.nonEmpty)
    val featuredEvents = EventGroup("Featured", guLiveEvents.getFeaturedEvents)
    val events = EventGroup("What's on", chronologicalSort(locationOpt.fold(allEvents)(allEventsByLocation)))

    normalCatalog.map(cat =>
    Ok(views.html.event.eventsList(
      cat,
      pageInfo,
      events,
      featuredEvents,
      locationFilterItems,
      locationOpt
    )))
  }

  def calendar = CachedAction { implicit request =>
    val locationOpt = request.getQueryString("location").filter(_.trim.nonEmpty)

    val calendarEvents =
      CalendarMonthDayGroup("Calendar", groupEventsByDayAndMonth(locationOpt.fold(allEvents)(allEventsByLocation)))

    Ok(views.html.event.calendar(
      PageInfo(title = s"${calendarEvents.title} | Events", url = request.path),
      calendarEvents,
      locationFilterItems,
      locationOpt
    ))
  }

  def listArchive = CachedAction { implicit request =>
    val calendarArchive =
      CalendarMonthDayGroup("Archive", groupEventsByDayAndMonth(allEventsInArchive)(implicitly[Ordering[LocalDate]].reverse))

    Ok(views.html.event.eventsListArchive(
      PageInfo(title = s"${calendarArchive.title} | Events", url = request.path),
      calendarArchive
    ))
  }

  def masterclassesList = CachedAction.async { implicit request =>
    val pageInfo = PageInfo(
      title = CopyConfig.copyTitleMasterclasses,
      url = request.path,
      description = Some(CopyConfig.copyDescriptionMasterclasses)
    )
    val eventGroup = EventGroup("Masterclasses", masterclassEvents.events)

    normalCatalog.map( cat =>
      Ok(views.html.event.masterclassesList(cat, pageInfo, eventGroup))
    )
  }

  def masterclassesListFilteredBy(rawTag: String, rawSubTag: String = "") = CachedAction.async { implicit request =>
    val pageInfo = PageInfo(
      title = CopyConfig.copyTitleMasterclasses,
      url = request.path,
      description = Some(CopyConfig.copyDescriptionMasterclasses)
    )
    val tag = decodeTag( if(rawSubTag.nonEmpty) rawSubTag else rawTag )
    val eventGroup = EventGroup("Masterclasses", masterclassEvents.getTaggedEvents(tag))
    normalCatalog.map(c =>
      Ok(views.html.event.masterclassesList(c, pageInfo, eventGroup, decodeTag(rawTag), decodeTag(rawSubTag)))
    )
  }
}

object WhatsOn extends WhatsOn {
  val guLiveEvents = GuardianLiveEventService
  val localEvents = LocalEventService
  val masterclassEvents = MasterclassEventService
}
