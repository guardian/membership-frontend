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
import model.{Tier, Member}
import configuration.Config
import play.api.libs.json.Reads

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

  private def get[A <: EBObject](url: String, params: (String, String)*)(implicit reads: Reads[A]): Future[A] =
    WS.url(s"$apiUrl/$url")
      .withQueryString("token" -> apiToken).withQueryString(params: _*)
      .get().map(extract[A])

  private def post[A <: EBObject](url: String, data: Map[String, Seq[String]])(implicit reads: Reads[A]): Future[A] =
    WS.url(s"$apiUrl/$url").withQueryString("token" -> apiToken).post(data).map(extract[A])

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

  private def getAllEvents: Future[Seq[EBEvent]] = getPaginated[EBEvent](apiEventListUrl)
  
  def getLiveEvents: Seq[EBEvent] = allEvents().filter { event =>
    event.getStatus == EBEventStatus.SoldOut || event.getStatus == EBEventStatus.Live
  }

  /**
   * scuzzy implementation to enable basic 'filtering by tag' - in this case, just matching the event name.
   */
  def getEventsTagged(tag: String) = getLiveEvents.filter(_.name.text.toLowerCase.contains(tag))

  def getEvent(id: String): Future[EBEvent] = get[EBEvent](s"events/$id")

  def createDiscount(member: Member, eventId: String): Future[EBDiscount] = {
    val uri = s"events/$eventId/discounts"
    val code = s"${member.userId}_$eventId"

    for {
      discounts <- getPaginated[EBDiscount](uri)
      discount <- discounts.find(_.code == code).map(Future.successful).getOrElse {
        post[EBDiscount](uri, Map(
          "discount.code" -> Seq(code),
          "discount.percent_off" -> Seq("20"),
          "discount.quantity_available" -> Seq("2")
        ))
      }
    } yield discount
  }
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

