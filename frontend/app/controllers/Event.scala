package controllers

import scala.concurrent.Future

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import services.{MemberService, EventbriteService}
import actions.AuthenticatedAction
import model.Eventbrite.EBError

import com.netaporter.uri.dsl._

trait Event extends Controller {

  val eventService: EventbriteService
  val memberService: MemberService

  def details(id: String) = CachedAction.async { implicit request =>
    eventService.getEvent(id)
      .map(event => Ok(views.html.event.page(event)))
      .recover { case error: EBError if error.status_code == NOT_FOUND => NotFound }
  }

  def list = CachedAction { implicit request =>
    Ok(views.html.event.list(eventService.getLiveEvents))
  }

  def listFilteredBy(urlTagText: String) = CachedAction { implicit request =>
    val tag = urlTagText.replace('-', ' ')
    Ok(views.html.event.list(eventService.getEventsTagged(tag)))
  }

  def buy(id: String) = AuthenticatedAction.async { implicit request =>
    for {
      event <- eventService.getEvent(id)
      discount <- memberService.createEventDiscount(request.user.id, event)
    } yield Found(event.url ? ("discount" -> discount.code))
  }
}

object Event extends Event {
  val eventService = EventbriteService
  val memberService = MemberService
}