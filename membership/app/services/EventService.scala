package services

import scala.concurrent.Future
import model.{EBResponse, EBEvent}
import play.api.libs.ws._
import model.EventbriteDeserializer._


trait EventService {
  def getAllEvents(): Future[Seq[EBEvent]]
}

trait EventBriteService extends EventService {
  implicit val context = scala.concurrent.ExecutionContext.Implicits.global

  val eventUrl:String
  val token:(String, String)

  override def getAllEvents(): Future[Seq[EBEvent]] ={
    val requestHolder = WS
      .url(eventUrl)
      .withQueryString(token)
    requestHolder.get().map(s => jsonMembershipEvent(s))
  }

  def jsonMembershipEvent(r:Response):Seq[EBEvent] = {
    r.json.as[EBResponse].events
  }
}

object EventBriteService extends EventBriteService{
  override val eventUrl: String = ""
  override val token: (String, String) = ("", "")
}


