package controllers

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import services.EventbriteService
import actions.AuthenticatedAction
import model.Eventbrite.EBError

trait Event extends Controller {

  val eventService: EventbriteService

  def details(id: String) = CachedAction.async {
    eventService.getEvent(id)
      .map(event => Ok(views.html.event.page(event)))
      .recover { case error: EBError if error.status_code == NOT_FOUND => NotFound }
  }

  def list = CachedAction {
    Ok(views.html.event.list(eventService.getLiveEvents))
  }

  def listFilteredBy(urlTagText: String) = CachedAction {
    val tag = urlTagText.replace('-', ' ')
    Ok(views.html.event.list(eventService.getEventsTagged(tag)))
  }

  def buy(id: String) = AuthenticatedAction.async {
    eventService.getEvent(id).map(event => Found(event.url))
  }

}

object Event extends Event {
  override val eventService: EventbriteService = EventbriteService
}