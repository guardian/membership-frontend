package services

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import akka.agent.Agent

import play.api.libs.ws._
import play.api.libs.iteratee.{Iteratee, Enumerator}
import play.api.libs.concurrent.Akka
import play.api.Logger

import model.Eventbrite._
import model.EventbriteDeserializer._
import model.Member
import configuration.Config
import play.api.libs.json.Reads
import scala.util.{Failure, Success, Try}

trait EventbriteService {

  val apiUrl: String
  val apiToken: String

  val apiEventListUrl: String

  val allEvents = Agent[Seq[EBEvent]](Nil)

  def refresh() {
    Logger.debug("Refreshing EventbriteService events")
    allEvents.sendOff(_ => Await.result(getAllEvents, 15.seconds))
  }

  private def extract[A <: EBObject](response: Response)(implicit reads: Reads[A]): A = {
    response.json.asOpt[A].getOrElse {
      throw response.json.asOpt[EBError].getOrElse(EBError("internal", "Unable to extract object", 500))
    }
  }

  private def get[A <: EBObject](url: String, params: (String, String)*)(implicit reads: Reads[A]): Future[A] = {
    val response = WS.url(s"$apiUrl/$url").withQueryString("token" -> apiToken).withQueryString(params: _*).get()
    val trial = Try(response.map(extract[A]))
    trial match {
      case Success(result) => result
      case Failure(e) => {
        response.onSuccess { case r => Logger.error(s"Eventbrite request $url - Response body : ${r.body}", e)}
        throw e
      }
    }
  }

  private def post[A <: EBObject](url: String, data: Map[String, Seq[String]])(implicit reads: Reads[A]): Future[A] =
    WS.url(s"$apiUrl/$url").withQueryString("token" -> apiToken).post(data).map(extract[A])

  private def getAllEvents: Future[Seq[EBEvent]] = {
    val enumerator = Enumerator.unfoldM(Option(1)) {
      _.map { nextPage =>
        for {
          response <- get[EBResponse](apiEventListUrl, "page" -> nextPage.toString)
        } yield Option((response.pagination.nextPageOpt, response.events))
      }.getOrElse(Future.successful(None))
    }

    enumerator(Iteratee.consume()).flatMap(_.run)
  }

  def getLiveEvents: Seq[EBEvent] = allEvents().filter { event =>
    event.getStatus == EBEventStatus.SoldOut || event.getStatus == EBEventStatus.Live
  }

  /**
   * scuzzy implementation to enable basic 'filtering by tag' - in this case, just matching the event name.
   */
  def getEventsTagged(tag: String) = getLiveEvents.filter(_.name.text.toLowerCase.contains(tag))

  def getEvent(id: String): Future[EBEvent] = get[EBEvent](s"events/$id")
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

