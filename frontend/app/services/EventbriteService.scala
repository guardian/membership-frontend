package services

import scala.concurrent.{Await, Future}
import concurrent.duration._
import model.{EBEventStatus, EBResponse, EBEvent}
import play.api.libs.ws._
import model.EventbriteDeserializer._
import scala.concurrent.ExecutionContext.Implicits.global
import configuration.Config
import play.api.libs.iteratee.{Iteratee, Enumerator}
import akka.agent.Agent
import play.api.libs.concurrent.Akka
import play.api.Logger

trait EventbriteService {

  val allEvents = Agent[Seq[EBEvent]](Nil)

  def refresh() {
    Logger.debug("Refreshing EventbriteService events")
    allEvents.sendOff(_ => Await.result(getAllEvents, 15.seconds))
  }

  val eventListUrl: String
  val eventUrl: String
  val token: (String, String)

  private def getAllEvents: Future[Seq[EBEvent]] = pagingEnumerator()(Iteratee.consume()).flatMap(_.run)

  def pagingEnumerator(): Enumerator[Seq[EBEvent]] = Enumerator.unfoldM(Option(1)) {
    _.map { nextPage =>
      for (response <- requestEventbriteEvents(nextPage)) yield Option((response.pagination.nextPageOpt, response.events))
    }.getOrElse(Future.successful(None))
  }

  def getLiveEvents: Future[Seq[EBEvent]] = Future.successful(allEvents()).map { events =>
    events.filter(event => event.getStatus == EBEventStatus.SoldOut || event.getStatus == EBEventStatus.Live)
  }

  def getEvent(id: String): Future[EBEvent] = eventbriteRequest(eventUrlWith(id)).map(asEBEvent(_))

  def requestEventbriteEvents(page: Int = 1): Future[EBResponse] = eventbriteRequest(eventListUrl, page).map(_.json.as[EBResponse])

  def eventbriteRequest(url: String, page: Int = 1): Future[Response] = WS.url(url).withQueryString(token, ("page", page.toString)).get()

  private def eventUrlWith(id: String) = eventUrl + s"/$id"

  private def asEBEvent(r: Response) = r.json.as[EBEvent]
}

object EventbriteService extends EventbriteService {
  val eventListUrl: String = Config.eventListUrl
  val eventUrl: String = Config.eventUrl
  val token: (String, String) = Config.eventToken


  import play.api.Play.current
  private implicit val system = Akka.system

  def start() {
    Logger.info("Starting EventbriteService background tasks")
    system.scheduler.schedule(5.seconds, 60.seconds) { refresh() }
  }
}

