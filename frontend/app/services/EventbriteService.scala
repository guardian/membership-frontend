package services

import com.gu.membership.util.WebServiceHelper
import play.api.cache.Cache

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.Future

import play.api.Logger
import play.api.Play.current
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.libs.json.Reads
import play.api.libs.ws._

import configuration.Config
import model.EventPortfolio
import model.Eventbrite._
import model.EventbriteDeserializer._
import model.RichEvent._
import monitoring.EventbriteMetrics
import utils.ScheduledTask

trait EventbriteService extends WebServiceHelper[EBObject, EBError] {
  val apiToken: String
  val maxDiscountQuantityAvailable: Int

  val wsUrl = Config.eventbriteApiUrl
  def wsPreExecute(req: WSRequestHolder): WSRequestHolder = req.withQueryString("token" -> apiToken)

  val refreshTimeAllEvents = new FiniteDuration(Config.eventbriteRefreshTime, SECONDS)
  lazy val eventsTask = ScheduledTask[Seq[RichEvent]]("Eventbrite events", Nil, 1.second, refreshTimeAllEvents) {
    for {
      events <- getPaginated[EBEvent]("users/me/owned_events?status=live")
      richEvents <- Future.sequence(events.map(mkRichEvent))
    } yield richEvents
  }

  val refreshTimeDraftEvents = new FiniteDuration(Config.eventbriteRefreshTime, SECONDS)
  lazy val draftEventsTask = ScheduledTask[Seq[RichEvent]]("Eventbrite draft events", Nil, 1.second, refreshTimeDraftEvents) {
    for {
      eventsDraft <- getPaginated[EBEvent]("users/me/owned_events?status=draft")
      richDraftEvents <- Future.sequence(eventsDraft.map(mkRichEvent))
    } yield richDraftEvents
  }

  val refreshTimeArchivedEvents = new FiniteDuration(Config.eventbriteRefreshTime, SECONDS)
  lazy val archivedEventsTask = ScheduledTask[Seq[RichEvent]]("Eventbrite archived events", Nil, 1.second, refreshTimeArchivedEvents) {
    for {
      eventsArchive <- get[EBResponse[EBEvent]]("users/me/owned_events?status=ended&order_by=start_desc")
      richEventsArchive <- Future.sequence(eventsArchive.data.map(mkRichEvent))
    } yield richEventsArchive
  }

  def start() {
    Logger.info("Starting EventbriteService background tasks")
    eventsTask.start()
    draftEventsTask.start()
    archivedEventsTask.start()
  }

  def events: Seq[RichEvent] = eventsTask.get()
  def eventsDraft: Seq[RichEvent] = draftEventsTask.get()
  def eventsArchive: Seq[RichEvent] = archivedEventsTask.get()

  def mkRichEvent(event: EBEvent): Future[RichEvent]
  def getFeaturedEvents: Seq[RichEvent]
  def getTaggedEvents(tag: String): Seq[RichEvent]

  private def getPaginated[T](url: String)(implicit reads: Reads[EBResponse[T]]): Future[Seq[T]] = {
    val enumerator = Enumerator.unfoldM(Option(1)) {
      _.map { nextPage =>
        for {
          response <- get[EBResponse[T]](url, "page" -> nextPage.toString)
        } yield Some((response.pagination.nextPageOpt, response.data))
      }.getOrElse(Future.successful(None))
    }

    enumerator(Iteratee.consume()).flatMap(_.run)
  }

  def getEventPortfolio: EventPortfolio = {
    val featuredEvents = getFeaturedEvents
    EventPortfolio(featuredEvents, events.diff(featuredEvents), Some(eventsArchive))
  }

  def getPreviewEvent(id: String): Future[RichEvent] = for {
    event <- get[EBEvent](s"events/$id")
    richEvent <- mkRichEvent(event)
  } yield richEvent

  def getBookableEvent(id: String): Option[RichEvent] = events.find(_.id == id)
  def getEvent(id: String): Option[RichEvent] = (events ++ eventsArchive).find(_.id == id)

  def createOrGetAccessCode(event: RichEvent, code: String, ticketClasses: Seq[EBTicketClass]): Future[EBAccessCode] = {
    val uri = s"events/${event.id}/access_codes"

    for {
      discounts <- getPaginated[EBAccessCode](uri)
      discount <- discounts.find(_.code == code).fold {
        post[EBAccessCode](uri, Map(
          "access_code.code" -> Seq(code),
          "access_code.quantity_available" -> Seq(maxDiscountQuantityAvailable.toString),
          "access_code.ticket_ids" -> Seq(ticketClasses.map(_.id).mkString(","))
        ))
      }(Future.successful)
    } yield discount
  }

  def getOrder(id: String): Future[EBOrder] = get[EBOrder](s"orders/$id")
}

object GuardianLiveEventService extends EventbriteService {
  val apiToken = Config.eventbriteApiToken
  val maxDiscountQuantityAvailable = 2

  val wsMetrics = new EventbriteMetrics("Guardian Live")

  val gridService = GridService(Config.gridConfig.url)
  val contentApiService = GuardianContentService


  val refreshTimePriorityEvents = new FiniteDuration(Config.eventbriteRefreshTimeForPriorityEvents, SECONDS)
  lazy val eventsOrderingTask = ScheduledTask[Seq[String]]("Event ordering", Nil, 1.second, refreshTimePriorityEvents) {
    for {
      ordering <- WS.url(Config.eventOrderingJsonUrl).get()
    } yield (ordering.json \ "order").as[Seq[String]]
  }

  def mkRichEvent(event: EBEvent): Future[RichEvent] = {

    val eventbriteContent = contentApiService.content(event.id)

    event.mainImageUrl.fold(Future.successful(GuLiveEvent(event, None, eventbriteContent))) { url =>
      gridService.getRequestedCrop(url).map(GuLiveEvent(event, _, eventbriteContent))
    }
  }

  override def getFeaturedEvents: Seq[RichEvent] = EventbriteServiceHelpers.getFeaturedEvents(eventsOrderingTask.get(), events)
  override def getTaggedEvents(tag: String): Seq[RichEvent] = events.filter(_.name.text.toLowerCase.contains(tag))

  override def start() {
    super.start()
    eventsOrderingTask.start()
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
  override def getTaggedEvents(tag: String): Seq[RichEvent] = events.filter(_.tags.contains(tag.toLowerCase))
}

object EventbriteServiceHelpers {

  def getFeaturedEvents(orderedIds: Seq[String], events: Seq[RichEvent]): Seq[RichEvent] = {
    val (orderedEvents, normalEvents) = events.partition { event => orderedIds.contains(event.id) }
    orderedEvents.sortBy { event => orderedIds.indexOf(event.id) } ++ normalEvents.filter(!_.isSoldOut).take(4 - orderedEvents.length)
  }
}

object EventbriteService {
  val services = Seq(GuardianLiveEventService, MasterclassEventService)

  implicit class RichEventProvider(event: RichEvent) {
    val service = event match {
      case _: GuLiveEvent => GuardianLiveEventService
      case _: MasterclassEvent => MasterclassEventService
    }
  }

  def getPreviewEvent(id: String): Future[RichEvent] = Cache.getOrElse[Future[RichEvent]](s"preview-event-$id", 2) {
    GuardianLiveEventService.getPreviewEvent(id)
  }

  def searchServices(fn: EventbriteService => Option[RichEvent]): Option[RichEvent] =
    services.flatMap { service => fn(service) }.headOption

  def getBookableEvent(id: String) = searchServices(_.getBookableEvent(id))
  def getEvent(id: String) = searchServices(_.getEvent(id))
}
