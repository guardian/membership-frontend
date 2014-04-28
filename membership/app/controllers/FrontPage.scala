package controllers

import play.api.mvc.{Action, Controller}
import services.{EventBriteService, EventService}
import scala.concurrent.ExecutionContext.Implicits.global

trait FrontPage extends Controller{

  val eventService: EventService

  def index = Action.async {
    eventService.getEvents().map{ events =>
      Ok(views.html.index(events))
    }
  }
}

object FrontPage extends FrontPage{
  override val eventService: EventService = new EventBriteService
}