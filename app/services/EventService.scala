package services

import scala.concurrent.Future
import model.{EBResponse, EBEvent}
import play.api.libs.ws._
import model.EventbriteDeserializer._
import scala.concurrent.ExecutionContext.Implicits.global
import com.typesafe.config.ConfigFactory


trait EventService {
  def getAllEvents(): Future[Seq[EBEvent]]
}

trait EventbriteService extends EventService {

  val eventUrl: String
  val token: (String, String)

  override def getAllEvents(): Future[Seq[EBEvent]] = {
    val requestHolder = WS
      .url(eventUrl)
      .withQueryString(token)
    requestHolder.get().map(s => jsonMembershipEvent(s))
  }

  def jsonMembershipEvent(r: Response): Seq[EBEvent] = {
    r.json.as[EBResponse].events
  }
}

object EventbriteService extends EventbriteService {
  val config = ConfigFactory.load()
  override val eventUrl: String = config.getString("eventbrite.user.events-url")
  override val token: (String, String) = ("token", config.getString("eventbrite.token"))
}


