package services

import com.github.nscala_time.time.OrderingImplicits._
import com.gu.membership.util.WebServiceHelper
import configuration.Config
import model.Eventbrite._
import model.EventbriteDeserializer._
import model.RichEvent._
import monitoring.EventbriteMetrics
import org.joda.time.{DateTime, Interval}
import play.api.Logger
import play.api.Play.current
import play.api.cache.Cache
import play.api.libs.json.Reads
import play.api.libs.ws._
import utils.ScheduledTask

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

trait EventbriteService extends WebServiceHelper[EBObject, EBError] {
  val apiToken: String
  val maxDiscountQuantityAvailable: Int

  val wsUrl = Config.eventbriteApiUrl
  def wsPreExecute(req: WSRequest): WSRequest = req.withQueryString("token" -> apiToken)

  def eventsTaskFor(status: String, refreshTime : FiniteDuration): ScheduledTask[Seq[RichEvent]] =
    ScheduledTask[Seq[RichEvent]](s"Eventbrite $status events", Nil, 1.second, refreshTime) {
      for {
        events <- getAll[EBEvent]("users/me/owned_events", List("status" -> status, "expand" -> EBEvent.expansions.mkString(",")))
        richEvents <- Future.traverse(events)(mkRichEvent)
      } yield richEvents
    }

  lazy val eventsTask = eventsTaskFor("live", Config.eventbriteRefreshTime.seconds)

  lazy val draftEventsTask =  eventsTaskFor("draft", Config.eventbriteRefreshTime.seconds)

  lazy val archivedEventsTask = eventsTaskFor("ended", 2.hours) // we keep archived events so as to not break old urls

  def start() {
    Logger.info("Starting EventbriteService background tasks")
    eventsTask.start(60.seconds)
    draftEventsTask.start(60.seconds)
    archivedEventsTask.start(60.seconds)
  }

  def events: Seq[RichEvent] = eventsTask.get()
  def eventsDraft: Seq[RichEvent] = draftEventsTask.get()
  def eventsArchive: Seq[RichEvent] = archivedEventsTask.get()

  def mkRichEvent(event: EBEvent): Future[RichEvent]
  def getFeaturedEvents: Seq[RichEvent]
  def getEvents: Seq[RichEvent]
  def getTaggedEvents(tag: String): Seq[RichEvent]
  def getPartnerEvents: Seq[RichEvent]
  def getEventsArchive: Option[Seq[RichEvent]] = Some(eventsArchive)

  private def getAll[T](url: String, params: Seq[(String, String)] = Seq.empty)(implicit reads: Reads[EBResponse[T]]): Future[Seq[T]] = {
    def getPage(page: Int) = get[EBResponse[T]](url, Seq("page" -> page.toString) ++ params:_*)

    for {
      initialResponse <- getPage(1)
      followingResponses: Seq[EBResponse[T]] <- Future.traverse(2 to initialResponse.pagination.page_count)(getPage)
    } yield (initialResponse +: followingResponses).flatMap(_.data)
  }

  def getPreviewEvent(id: String): Future[RichEvent] = for {
    event <- get[EBEvent](s"events/$id", "expand" -> EBEvent.expansions.mkString(","))
    richEvent <- mkRichEvent(event)
  } yield richEvent

  def getBookableEvent(id: String): Option[RichEvent] = events.find(_.id == id)
  def getEvent(id: String): Option[RichEvent] = (events ++ eventsArchive).find(_.id == id)

  def getEventsByIds(ids: Seq[String]): Seq[RichEvent] = events.filter(e => ids.contains(e.event.id))
  def getLimitedAvailability: Seq[RichEvent] = events.filter(_.event.isLimitedAvailability)
  def getRecentlyCreated(start: DateTime): Seq[RichEvent] = events.filter(_.created.isAfter(start))
  def getSortedByCreationDate: Seq[RichEvent] = events.sortBy(_.created.toDateTime)(Ordering[DateTime].reverse)
  def getEventsBetween(interval: Interval): Seq[RichEvent] = events.filter(event => interval.contains(event.start))

  def createOrGetAccessCode(event: RichEvent, code: String, ticketClasses: Seq[EBTicketClass]): Future[Option[EBAccessCode]] = {
      val uri = s"events/${event.id}/access_codes"

      for {
        discounts <- getAll[EBAccessCode](uri) if ticketClasses.nonEmpty
        discount <- discounts.find(_.code == code).fold {
          post[EBAccessCode](uri, Map(
            "access_code.code" -> Seq(code),
            "access_code.quantity_available" -> Seq(maxDiscountQuantityAvailable.toString),
            "access_code.ticket_ids" -> Seq(ticketClasses.map(_.id).mkString(","))
          ))
        }(Future.successful)
      } yield Some(discount)
  } recover { case _: NoSuchElementException => None }

  def getOrder(id: String): Future[EBOrder] = get[EBOrder](s"orders/$id", "expand" -> EBOrder.expansions.mkString(","))
}

abstract class LiveService extends EventbriteService {
  val gridService = GridService(Config.gridConfig.url)
  val contentApiService = GuardianContentService

  def gridImageFor(event: EBEvent) =
    event.mainImageUrl.fold[Future[Option[GridImage]]](Future.successful(None))(gridService.getRequestedCrop)
}

object GuardianLiveEventService extends LiveService {
  val apiToken = Config.eventbriteApiToken
  // For partner/patrons with free event tickets benefits, we generate a discount code which unlocks a combination of
  // maximum 2 discounted tickets and 1 complimentary ticket.
  // The maxDiscountQuantityAvailable value is used to set the Access code 'quantity_available' attribute (i.e. the
  // maximum number of tickets that can be purchased with a given code).
  //
  // see https://www.eventbrite.com/developer/v3/formats/event/#ebapi-access-code
  val maxDiscountQuantityAvailable = 3
  val wsMetrics = new EventbriteMetrics("Guardian Live")

  val refreshTimePriorityEvents = new FiniteDuration(Config.eventbriteRefreshTimeForPriorityEvents, SECONDS)
  lazy val eventsOrderingTask = ScheduledTask[Seq[String]]("Event ordering", Nil, 1.second, refreshTimePriorityEvents) {
    for {
      ordering <- WS.url(Config.eventOrderingJsonUrl).get()
    } yield (ordering.json \ "order").as[Seq[String]]
  }

  def mkRichEvent(event: EBEvent): Future[RichEvent] = for { gridImageOpt <- gridImageFor(event) }
    yield GuLiveEvent(event, gridImageOpt, contentApiService.content(event.id))

  override def getFeaturedEvents: Seq[RichEvent] = EventbriteServiceHelpers.getFeaturedEvents(eventsOrderingTask.get(), events)
  override def getEvents: Seq[RichEvent] = events.diff(getFeaturedEvents ++ getPartnerEvents)
  override def getTaggedEvents(tag: String): Seq[RichEvent] = events.filter(_.name.text.toLowerCase.contains(tag))
  override def getPartnerEvents: Seq[RichEvent] = events.filter(_.providerOpt.isDefined)
  override def start() {
    super.start()
    eventsOrderingTask.start()
  }
}

object LocalEventService extends LiveService {
  val apiToken = Config.eventbriteLocalApiToken
  val maxDiscountQuantityAvailable = 2
  val wsMetrics = new EventbriteMetrics("Local")

  def mkRichEvent(event: EBEvent): Future[RichEvent] =  for { gridImageOpt <- gridImageFor(event) }
    yield LocalEvent(event, gridImageOpt, contentApiService.content(event.id))

  override def getFeaturedEvents: Seq[RichEvent] = EventbriteServiceHelpers.getFeaturedEvents(Nil, events)
  override def getEvents: Seq[RichEvent] = events
  override def getTaggedEvents(tag: String): Seq[RichEvent] = events.filter(_.name.text.toLowerCase.contains(tag))
  override def getPartnerEvents: Seq[RichEvent] = Seq.empty

  override def start() {
    super.start()
  }
}

case class MasterclassEventServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}

object MasterclassEventsProvider {
  val MasterclassesWithAvailableMemberDiscounts: (RichEvent) => Boolean =
    _.internalTicketing.exists(_.memberDiscountOpt.exists(!_.isSoldOut))
}

object MasterclassEventService extends EventbriteService {
  import MasterclassEventsProvider._

  val apiToken = Config.eventbriteMasterclassesApiToken
  val maxDiscountQuantityAvailable = 1

  val wsMetrics = new EventbriteMetrics("Masterclasses")

  val contentApiService = GuardianContentService

  override def events: Seq[RichEvent] = super.events.filter(MasterclassesWithAvailableMemberDiscounts)

  def mkRichEvent(event: EBEvent): Future[RichEvent] = {
    val masterclassData = contentApiService.masterclassContent(event.id)
    //todo change this to have link to weburl
    Future.successful(MasterclassEvent(event, masterclassData))
  }

  override def getFeaturedEvents: Seq[RichEvent] = Nil
  override def getEvents: Seq[RichEvent] = events
  override def getTaggedEvents(tag: String): Seq[RichEvent] = events.filter(_.tags.contains(tag.toLowerCase))
  override def getPartnerEvents: Seq[RichEvent] = Seq.empty
}

object EventbriteServiceHelpers {

  def getFeaturedEvents(orderedIds: Seq[String], events: Seq[RichEvent]): Seq[RichEvent] = {
    val (orderedEvents, normalEvents) = events.partition { event => orderedIds.contains(event.id) }
    orderedEvents.sortBy { event => orderedIds.indexOf(event.id) } ++ normalEvents.filter(!_.isSoldOut).take(4 - orderedEvents.length)
  }
}

trait EventbriteCollectiveServices {
  val services = Seq(GuardianLiveEventService, LocalEventService, MasterclassEventService)

  implicit class RichEventProvider(event: RichEvent) {
    val service = event match {
      case _: GuLiveEvent => GuardianLiveEventService
      case _: LocalEvent => LocalEventService
      case _: MasterclassEvent => MasterclassEventService
    }
  }

  def getPreviewEvent(id: String): Future[RichEvent] = Cache.getOrElse[Future[RichEvent]](s"preview-event-$id", 2) {
    GuardianLiveEventService.getPreviewEvent(id)
  }

  def getPreviewLocalEvent(id: String): Future[RichEvent] = Cache.getOrElse[Future[RichEvent]](s"preview-event-$id", 2) {
    LocalEventService.getPreviewEvent(id)
  }

  def getPreviewMasterclass(id: String): Future[RichEvent] = Cache.getOrElse[Future[RichEvent]](s"preview-event-$id", 2) {
    MasterclassEventService.getPreviewEvent(id)
  }

  def searchServices(fn: EventbriteService => Option[RichEvent]): Option[RichEvent] =
    services.flatMap { service => fn(service) }.headOption

  def getBookableEvent(id: String) = searchServices(_.getBookableEvent(id))
  def getEvent(id: String) = searchServices(_.getEvent(id))
}

object EventbriteService extends EventbriteCollectiveServices
