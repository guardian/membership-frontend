package controllers

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import services.{MemberService, EventbriteService}
import actions.AuthenticatedAction
import model.Eventbrite.EBError
import scala.concurrent.Future

trait Event extends Controller {

  val eventService: EventbriteService
  val memberService: MemberService

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

  def buy(id: String) = AuthenticatedAction.async { implicit request =>
    for {
      event <- eventService.getEvent(id)
      discountOpt <- AwsMemberTable.get(request.user.id).map { member =>
        eventService.createDiscount(member, event.id)
      }.getOrElse(Future.successful(None))
    } yield Found(event.url + discountOpt.fold("")(discount => s"?discount=${discount.code}"))
  }
}

object Event extends Event {
  override val eventService: EventbriteService = EventbriteService
}