package controllers

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import services.EventbriteService

trait Event extends Controller {

  val eventService: EventbriteService

  def renderEventPage(id: String) = Action.async {
    eventService.getEvent(id).map(event => Ok(views.html.events.eventPage(event)))
  }

  def renderEventsIndex = Action.async {
    eventService.getAllEvents.map(events => Ok(views.html.events.eventsIndex(events)))
  }

}

object Event extends Event {
  override val eventService: EventbriteService = EventbriteService
}