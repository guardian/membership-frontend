package controllers

import model.EventPortfolio

import scala.concurrent.Future

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import actions.Functions.{authenticated, memberRefiner}
import actions.Fallbacks.notYetAMemberOn
import services.{MemberService, EventbriteService}

import com.netaporter.uri.dsl._

trait Event extends Controller {

  val eventService: EventbriteService
  val memberService: MemberService

  val BuyAction = NoCacheAction andThen authenticated(onUnauthenticated = notYetAMemberOn(_)) andThen memberRefiner()

  def details(id: String) = CachedAction {
    eventService.getEvent(id).map {
      event => Ok(views.html.event.page(event))
    }.getOrElse(NotFound)
  }

  def list = CachedAction { implicit request =>
    Ok(views.html.event.list(eventService.getEventPortfolio))
  }

  def listFilteredBy(urlTagText: String) = CachedAction { implicit request =>
    val tag = urlTagText.replace('-', ' ')
    Ok(views.html.event.list(EventPortfolio(Seq.empty, eventService.getEventsTagged(tag))))
  }

  def buy(id: String) = BuyAction.async { implicit request =>
    eventService.getEvent(id).map {
      event =>
        for {
          discountOpt <- memberService.createDiscountForMember(request.member, event)
        } yield Found(event.url ? ("discount" -> discountOpt.map(_.code)))
    }.getOrElse(Future.successful(NotFound))
  }
}

object Event extends Event {
  val eventService = EventbriteService
  val memberService = MemberService
}
