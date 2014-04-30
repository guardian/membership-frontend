package controllers

import model.EventbriteDeserializer._
import play.api.mvc._
import play.api.libs.ws._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import model.EBEvent
import services.{EventbriteService, EventService}

trait EventController extends Controller {

  def getEventDetails(id: String) = {
    val url = s"https://www.eventbriteapi.com/v3/events/$id/?token=***REMOVED***"

    val request = WS.url(url).withQueryString(("token", "***REMOVED***"))

    for {
      response <- request.get()
    } yield {
      response.json.as[EBEvent]
    }
  }

  def renderEventPage(id: String) = Action.async {

    for {
      event <- getEventDetails(id)
    } yield {
      Ok(views.html.events.eventPage(event))
    }

  }

  val eventService: EventService

  def renderEventsIndex = Action.async {
    eventService.getAllEvents().map{ events =>
      Ok(views.html.events.eventsIndex(events))
    }
  }

}

object EventController extends EventController{
  override val eventService: EventService = EventbriteService
}