package services

import akka.agent.Agent
import configuration.Config
import model.EventPortfolio
import model.Eventbrite._
import model.EventbriteDeserializer._
import monitoring.EventbriteMetrics
import play.api.Logger
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.iteratee.{Enumerator, Iteratee}
import play.api.libs.json.Reads
import play.api.libs.ws._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

trait EventbriteService extends utils.WebServiceHelper[EBObject, EBError] {
  val apiToken: String

  val wsUrl = Config.eventbriteApiUrl

  def wsPreExecute(req: WSRequestHolder): WSRequestHolder = req.withQueryString("token" -> apiToken)

  def scheduleAgentRefresh[T](agent: Agent[T], refresher: => Future[T], intervalPeriod: FiniteDuration) = {
    Akka.system.scheduler.schedule(1.second, intervalPeriod) {
      agent.sendOff(_ => Await.result(refresher, 25.seconds))
    }
  }
  val refreshTimeAllEvents = new FiniteDuration(Config.eventbriteRefreshTime, SECONDS)
  val refreshTimeArchivedEvents = new FiniteDuration(Config.eventbriteRefreshTime, SECONDS)
  val refreshTimePriorityEvents = new FiniteDuration(Config.eventbriteRefreshTimeForPriorityEvents, SECONDS)

  lazy val allEvents = Agent[Seq[RichEvent]](Seq.empty)
  lazy val archivedEvents = Agent[Seq[RichEvent]](Seq.empty)
  lazy val priorityOrderedEventIds = Agent[Seq[String]](Seq.empty)

  def events: Seq[RichEvent] = allEvents.get()
  def eventsArchive: Seq[RichEvent] = archivedEvents.get()
  def priorityEventOrdering: Seq[String] = priorityOrderedEventIds.get()

  def mkRichEvent(event: EBEvent): Future[RichEvent]

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

  protected def getLiveEvents: Future[Seq[RichEvent]] = for {
    events <- getPaginated[EBEvent]("users/me/owned_events?status=live")
    richEvents <- Future.sequence(events.map(mkRichEvent))
  } yield richEvents

  // only load 1 page of past events (masterclasses have 800+ of them)
  protected def getArchivedEvents: Future[Seq[RichEvent]] = for {
    eventsArchive <- get[EBResponse[EBEvent]]("users/me/owned_events?status=ended&order_by=start_desc")
    richEventsArchive <- Future.sequence(eventsArchive.data.map(mkRichEvent))
  } yield richEventsArchive

  def getEventPortfolio: EventPortfolio = {
    val priorityIds = priorityEventOrdering
    val (priorityEvents, normal) = events.partition(e => priorityIds.contains(e.id))
    EventPortfolio(priorityEvents.sortBy(e => priorityIds.indexOf(e.id)), normal, Some(eventsArchive))
  }

  /**
   * scuzzy implementation to enable basic 'filtering by tag' - in this case, just matching the event name.
   */
  def getEventsTagged(tag: String) = events.filter(_.name.text.toLowerCase.contains(tag))

  def getBookableEvent(id: String): Option[RichEvent] = events.find(_.id == id)
  def getAllEvents(id: String): Option[RichEvent] = {
    (events++eventsArchive).find(_.id == id)
  }

  def createOrGetAccessCode(event: RichEvent, code: String, ticketClasses: Seq[EBTicketClass]): Future[EBAccessCode] = {
    val uri = s"events/${event.id}/access_codes"

    for {
      discounts <- getPaginated[EBAccessCode](uri)
      discount <- discounts.find(_.code == code).fold {
        post[EBAccessCode](uri, Map(
          "access_code.code" -> Seq(code),
          "access_code.quantity_available" -> Seq(event.maxDiscounts.toString),
          "access_code.ticket_ids" -> Seq(ticketClasses.head.id) // TODO: support multiple ticket classes when Eventbrite fix their API
        ))
      }(Future.successful)
    } yield discount
  }

  def createOrGetDiscount(event: RichEvent, code: String): Future[EBDiscount] = {
    val uri = s"events/${event.id}/discounts"

    for {
      discounts <- getPaginated[EBDiscount](uri)
      discount <- discounts.find(_.code == code).map(Future.successful).getOrElse {
        post[EBDiscount](uri, Map(
          "discount.code" -> Seq(code),
          "discount.percent_off" -> Seq("20"),
          "discount.quantity_available" -> Seq(event.maxDiscounts.toString)
        ))
      }
    } yield discount
  }

  def getOrder(id: String): Future[EBOrder] = get[EBOrder](s"orders/$id")
}

object GuardianLiveEventService extends EventbriteService {
  val apiToken = Config.eventbriteApiToken

  val wsMetrics = new EventbriteMetrics("Guardian Live")
  val gridService = GridService

  private def getPriorityEventIds: Future[Seq[String]] =  for {
    ordering <- WS.url(Config.eventOrderingJsonUrl).get()
  } yield (ordering.json \ "order").as[Seq[String]]

  def mkRichEvent(event: EBEvent): Future[RichEvent] = {

    val imageOpt = event.description.flatMap(_.mainImageUrl)

    imageOpt.fold(Future.successful(GuLiveEvent(event, None))) { url =>
      gridService.getRequestedCrop(url).map(GuLiveEvent(event, _))
    }
  }

  def start() {
    Logger.info("Starting EventbriteService GuardianLive background tasks")
    scheduleAgentRefresh(allEvents, getLiveEvents, refreshTimeAllEvents)
    scheduleAgentRefresh(priorityOrderedEventIds, getPriorityEventIds, refreshTimePriorityEvents)
    scheduleAgentRefresh(archivedEvents, getArchivedEvents, refreshTimeArchivedEvents)
  }
}

object MasterclassEventService extends EventbriteService {
  import MasterclassEventServiceHelpers._

  val masterclassDataService = MasterclassDataService

  val apiToken = Config.eventbriteMasterclassesApiToken

  val wsMetrics = new EventbriteMetrics("Masterclasses")

  override def events: Seq[RichEvent] = allEvents.map(availableEvents).get()

  def mkRichEvent(event: EBEvent): Future[RichEvent] = {
    val masterclassData = masterclassDataService.getData(event.id)
    Future.successful(MasterclassEvent(event, masterclassData))
  }

  override def getEventsTagged(tag: String): Seq[RichEvent] = events.filter(_.tags.contains(tag.toLowerCase))

  def start() {
    Logger.info("Starting EventbriteService Masterclasses background tasks")
    scheduleAgentRefresh(allEvents, getLiveEvents, refreshTimeAllEvents)
    scheduleAgentRefresh(archivedEvents, getArchivedEvents, refreshTimeArchivedEvents)
  }
}

object MasterclassEventServiceHelpers {
  def availableEvents(events: Seq[RichEvent]): Seq[RichEvent] =
    events.filter(_.memberTickets.exists { t => t.quantity_sold < t.quantity_total } )
}

object EventbriteService {
  def getBookableEvent(id: String): Option[RichEvent] =
    GuardianLiveEventService.getBookableEvent(id) orElse MasterclassEventService.getBookableEvent(id)

  def getEvent(id: String): Option[RichEvent] =
    GuardianLiveEventService.getAllEvents(id) orElse MasterclassEventService.getAllEvents(id)

  def getService(event: RichEvent): EventbriteService =
    event match {
      case _: GuLiveEvent => GuardianLiveEventService
      case _: MasterclassEvent => MasterclassEventService
    }
}
