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

  val apiUrl: String
  val apiToken: String

  val apiEventListUrl: String

  private def getAllEvents: Future[Seq[EBEvent]] = pagingEnumerator()(Iteratee.consume()).flatMap(_.run)

  def pagingEnumerator(): Enumerator[Seq[EBEvent]] = Enumerator.unfoldM(Option(1)) {
    _.map { nextPage =>
      for (response <- requestEventbriteEvents(nextPage)) yield Option((response.pagination.nextPageOpt, response.events))
    }.getOrElse(Future.successful(None))
  }

  def getLiveEvents: Future[Seq[EBEvent]] = Future.successful(allEvents()).map { events =>
    events.filter(event => event.getStatus == EBEventStatus.SoldOut || event.getStatus == EBEventStatus.Live)
  }

  /**
   * scuzzy implementation to enable basic 'filtering by tag' - in this case, just matching the event name.
   */
  def getEventsTagged(tag: String) = getLiveEvents.map(_.filter(_.name.text.toLowerCase().contains(tag)))

  def getEvent(id: String): Future[EBEvent] = eventbriteRequest(s"events/$id").map(_.json.as[EBEvent])

  def requestEventbriteEvents(page: Int = 1): Future[EBResponse] =
    eventbriteRequest(apiEventListUrl, page).map(_.json.as[EBResponse])

  def eventbriteRequest(url: String, page: Int = 1): Future[Response] =
    WS.url(s"$apiUrl/$url").withQueryString("token" -> apiToken, "page" -> page.toString).get()
}

object EventbriteService extends EventbriteService {
  val apiUrl = Config.eventbriteApiUrl
  val apiToken = Config.eventbriteApiToken
  val apiEventListUrl = Config.eventbriteApiEventListUrl


  import play.api.Play.current
  private implicit val system = Akka.system

  def start() {
    Logger.info("Starting EventbriteService background tasks")
    system.scheduler.schedule(5.seconds, 60.seconds) { refresh() }
  }
}

