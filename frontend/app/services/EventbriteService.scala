package services

import scala.concurrent.Future
import model.{ EBResponse, EBEvent }
import play.api.libs.ws._
import model.EventbriteDeserializer._
import scala.concurrent.ExecutionContext.Implicits.global
import configuration.Config
import play.api.libs.iteratee.{Iteratee, Enumerator}

trait EventbriteService {

  val eventListUrl: String
  val eventUrl: String
  val token: (String, String)

  def getAllEvents: Future[Seq[EBEvent]] = pagingEnumerator()(Iteratee.consume()).flatMap(_.run)

  def pagingEnumerator(): Enumerator[Seq[EBEvent]] = Enumerator.unfoldM(Option(1)) {
      _.fold(Future.successful[Option[(Option[Int], Seq[model.EBEvent])]](None)) { nextPage =>
        for (response <- requestEventbriteEvents(nextPage)) yield Option((response.pagination.nextPageOpt, response.events))
      }
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
}

