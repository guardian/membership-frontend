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
import utils.ScheduledTask

trait EventbriteService extends utils.WebServiceHelper[EBObject, EBError] {
  val apiToken: String

  val wsUrl = Config.eventbriteApiUrl
  val wsMetrics = EventbriteMetrics

  def wsPreExecute(req: WSRequestHolder): WSRequestHolder = req.withQueryString("token" -> apiToken)

  def events: Seq[RichEvent]
  def eventsArchive: Seq[RichEvent]
  def priorityEventOrdering: Seq[String]
  def mkRichEvent(event: EBEvent): RichEvent

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
  } yield events.map(mkRichEvent)

  protected def getArchivedEvents: Future[Seq[RichEvent]] = for {
    eventsArchive <- getPaginated[EBEvent]("users/me/owned_events?status=ended")
  } yield eventsArchive.map(mkRichEvent)

  def getEventPortfolio: EventPortfolio = {
    val priorityIds = priorityEventOrdering
    val (priorityEvents, normal) = events.partition(e => priorityIds.contains(e.id))

    EventPortfolio(priorityEvents.sortBy(e => priorityIds.indexOf(e.id)), normal)
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

  val refreshTimeAllEvents = new FiniteDuration(Config.eventbriteRefreshTime, SECONDS)
  val refreshTimePriorityEvents = new FiniteDuration(Config.eventbriteRefreshTimeForPriorityEvents, SECONDS)
  val refreshTimeArchivedEvents = new FiniteDuration(Config.eventbriteRefreshTime, SECONDS)

  lazy val allEvents = Agent[Seq[RichEvent]](Seq.empty)
  lazy val archivedEvents = Agent[Seq[RichEvent]](Seq.empty)
  lazy val priorityOrderedEventIds = Agent[Seq[String]](Seq.empty)

  private def getPriorityEventIds: Future[Seq[String]] =  for {
    ordering <- WS.url(Config.eventOrderingJsonUrl).get()
  } yield (ordering.json \ "order").as[Seq[String]]

  def events: Seq[RichEvent] = allEvents.get()
  def eventsArchive: Seq[RichEvent] = archivedEvents.get()
  def priorityEventOrdering: Seq[String] = priorityOrderedEventIds.get()
  def mkRichEvent(event: EBEvent): RichEvent = GuLiveEvent(event)

  def start() {
    def scheduleAgentRefresh[T](agent: Agent[T], refresher: => Future[T], intervalPeriod: FiniteDuration) = {
      Akka.system.scheduler.schedule(1.second, intervalPeriod) {
        agent.sendOff(_ => Await.result(refresher, 15.seconds))
      }
    }
    Logger.info("Starting EventbriteService background tasks")
    scheduleAgentRefresh(allEvents, getLiveEvents, refreshTimeAllEvents)
    scheduleAgentRefresh(priorityOrderedEventIds, getPriorityEventIds, refreshTimePriorityEvents)
    scheduleAgentRefresh(archivedEvents, getArchivedEvents, refreshTimeArchivedEvents)
  }
}

object MasterclassEventServiceHelpers {
  def availableEvents(events: Seq[RichEvent]): Seq[RichEvent] =
    events.filter(_.memberTickets.exists { t => t.quantity_sold < t.quantity_total } )
}

object MasterclassEventService extends EventbriteService with ScheduledTask[Seq[RichEvent]] {
  import MasterclassEventServiceHelpers._

  val masterclassDataService = MasterclassDataService

  val apiToken = Config.eventbriteMasterclassesApiToken

  val initialValue = Nil
  val interval = new FiniteDuration(Config.eventbriteRefreshTime, SECONDS)
  val initialDelay = 0.seconds

  def refresh(): Future[Seq[RichEvent]] = getLiveEvents.map(availableEvents)

  def events: Seq[RichEvent] = agent.get()
  def eventsArchive: Seq[RichEvent] = Nil
  def priorityEventOrdering: Seq[String] = Nil
  def mkRichEvent(event: EBEvent): RichEvent = {
    val masterclassData = masterclassDataService.getData(event.id)
    MasterclassEvent(event, masterclassData)
  }

  override def getEventsTagged(tag: String): Seq[RichEvent] = events.filter(_.tags.contains(tag.toLowerCase))
}

object EventbriteService {
  def getBookableEvent(id: String): Option[RichEvent] =
    GuardianLiveEventService.getBookableEvent(id) orElse MasterclassEventService.getBookableEvent(id)

  def getEvent(id: String): Option[RichEvent] =
    GuardianLiveEventService.getAllEvents(id) orElse MasterclassEventService.getAllEvents(id)
}
