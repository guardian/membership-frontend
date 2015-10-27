package services.eventbrite

import com.github.nscala_time.time.OrderingImplicits._
import configuration.Config
import model.Eventbrite._
import model.EventbriteDeserializer._
import model.RichEvent._
import org.joda.time.{DateTime, Interval}
import play.api.Logger
import utils.ScheduledTask
import utils.StringUtils._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._


trait EventbriteCache  {
  val client : EventbriteClient

  def eventsTaskFor(status: String, refreshTime : FiniteDuration): ScheduledTask[Seq[RichEvent]] =
    ScheduledTask[Seq[RichEvent]](s"Eventbrite $status events", Nil, 1.second, refreshTime) {
      for {
        events <- client.getAll[EBEvent]("users/me/owned_events", List("status" -> status, "expand" -> EBEvent.expansions.mkString(",")))
        richEvents <- Future.traverse(events)(mkRichEvent)
      } yield richEvents
    }

  private lazy val eventsTask = eventsTaskFor("live", Config.eventbriteRefreshTime.seconds)
  private lazy val draftEventsTask =  eventsTaskFor("draft", Config.eventbriteRefreshTime.seconds)
  private lazy val archivedEventsTask = eventsTaskFor("ended", 2.hours) // we keep archived events so as to not break old urls

  def start() {
    Logger.info("Starting EventbriteService background tasks")
    eventsTask.start(60.seconds)
    draftEventsTask.start(60.seconds)
    archivedEventsTask.start(60.seconds)
  }

  def events: Seq[RichEvent] = eventsTask.get()
  def eventsDraft: Seq[RichEvent] = draftEventsTask.get()
  def eventsArchive: Seq[RichEvent] = archivedEventsTask.get()

  protected def mkRichEvent(event: EBEvent): Future[RichEvent]

  def getFeaturedEvents: Seq[RichEvent]
  def getTaggedEvents(tag: String): Seq[RichEvent] = Seq.empty
  def getEventsArchive: Option[Seq[RichEvent]] = Some(eventsArchive)
  def getPartnerEvents: Seq[RichEvent] = events.filter(_.providerOpt.isDefined)


  def getPreviewEvent(id: String): Future[RichEvent] = for {
    event <- client.get[EBEvent](s"events/$id", "expand" -> EBEvent.expansions.mkString(","))
    richEvent <- mkRichEvent(event)
  } yield richEvent

  def getBookableEvent(id: String): Option[RichEvent] = events.find(_.id == id)
  def getEvent(id: String): Option[RichEvent] = (events ++ eventsArchive).find(_.id == id)

  def getEventsByIds(ids: Seq[String]): Seq[RichEvent] = events.filter(e => ids.contains(e.event.id))
  def getLimitedAvailability: Seq[RichEvent] = events.filter(_.event.isLimitedAvailability)
  def getRecentlyCreated(start: DateTime): Seq[RichEvent] = events.filter(_.created.isAfter(start))
  def getSortedByCreationDate: Seq[RichEvent] = events.sortBy(_.created.toDateTime)(Ordering[DateTime].reverse)
  def getEventsBetween(interval: Interval): Seq[RichEvent] = events.filter(event => interval.contains(event.start))

  def getEventsByLocation(slug: String): Seq[RichEvent] = events.filter(_.venue.address.flatMap(_.city).exists(c => slugify(c) == slug))

}

