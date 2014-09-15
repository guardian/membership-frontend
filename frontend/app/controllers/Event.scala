package controllers

import model.{EventPortfolio, PageInfo}

import scala.concurrent.Future

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import actions.Functions.{authenticated, memberRefiner}
import actions.Fallbacks.notYetAMemberOn
import services.{MemberService, EventbriteService}
import configuration.Config

import com.netaporter.uri.dsl._

trait Event extends Controller {

  val eventService: EventbriteService
  val memberService: MemberService

  val BuyAction = NoCacheAction andThen authenticated(onUnauthenticated = notYetAMemberOn(_)) andThen memberRefiner()

  def details(id: String) = CachedAction { implicit request =>
    eventService.getEvent(id).map { event =>
      val pageInfo = PageInfo(
        event.name.text,
        request.path,
        Some(eventDescription),
        Some(Config.eventImageFullPath(event.id))
        Some(event.venue.name.getOrElse("") + ", " + event.eventAddressLine),
      )
      Ok(views.html.event.page(event, pageInfo))
    }.getOrElse(NotFound)
  }

  def list = CachedAction { implicit request =>
    val pageInfo = PageInfo(
      Config.copyTitleEvents,
      request.path,
      Some(Config.copyDescriptionEvents)
    )
    Ok(views.html.event.list(eventService.getEventPortfolio, pageInfo))
  }

  def listFilteredBy(urlTagText: String) = CachedAction { implicit request =>
    val tag = urlTagText.replace('-', ' ')
    val pageInfo = PageInfo(
      Config.copyTitleEvents,
      request.path,
      Some(Config.copyDescriptionEvents)
    )
    Ok(views.html.event.list(EventPortfolio(Seq.empty, eventService.getEventsTagged(tag)), pageInfo))
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
