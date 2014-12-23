package controllers

import actions.AnyMemberTierRequest
import com.gu.membership.salesforce.Member
import com.gu.membership.util.Timing
import model.Eventbrite.{GuLiveEvent, RichEvent, EBEvent, MasterclassEvent}
import model.{TicketSaleDates, Eventbrite, EventPortfolio, PageInfo}
import monitoring.{Metrics, EventbriteMetrics}
import org.joda.time.Instant

import scala.concurrent.Future

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import actions.Functions._
import actions.Fallbacks._
import services.{MasterclassEventService, GuardianLiveEventService, MemberService, EventbriteService}
import configuration.{Config, CopyConfig}

import com.netaporter.uri.dsl._
import views.support.Dates._
import model.EventPortfolio
import play.api.mvc.Result

trait Event extends Controller {
  val guLiveEvents: EventbriteService
  val masterclassEvents: EventbriteService

  val memberService: MemberService

  private def metrics(event: RichEvent) = {
    event match {
      case _: GuLiveEvent => guLiveEvents.wsMetrics
      case _: MasterclassEvent => masterclassEvents.wsMetrics
    }
  }

  private def recordBuyIntention(eventId: String) = new ActionBuilder[Request] {
    override def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] = {
      EventbriteService.getEvent(eventId).map { event =>
        Timing.record(metrics(event), "buy-action-invoked") {
          block(request)
        }
      }.getOrElse(Future.successful(NotFound))
    }
  }

  private def BuyAction(id: String) = NoCacheAction andThen recordBuyIntention(id) andThen
    authenticated(onUnauthenticated = notYetAMemberOn(_)) andThen memberRefiner()

  def details(id: String) = CachedAction { implicit request =>
    EventbriteService.getEvent(id).map { event =>
      val pageInfo = PageInfo(
        event.name.text,
        request.path,
        event.description.map(_.blurb),
        Some(event.socialImgUrl)
      )
      Ok(views.html.event.page(event, pageInfo))
    }.getOrElse(Redirect(Config.guardianMembershipUrl))
  }

  def masterclasses = CachedAction { implicit request =>
    val pageInfo = PageInfo(
      CopyConfig.copyTitleMasterclasses,
      request.path,
      Some(CopyConfig.copyDescriptionMasterclasses)
    )
    Ok(views.html.event.masterclass(masterclassEvents.getEventPortfolio, pageInfo))
  }

  def masterclassesByTag(rawTag: String, rawSubTag: String = "") = CachedAction { implicit request =>
    val tag = MasterclassEvent.decodeTag( if(rawSubTag.nonEmpty) rawSubTag else rawTag )
    val pageInfo = PageInfo(
      CopyConfig.copyTitleMasterclasses,
      request.path,
      Some(CopyConfig.copyDescriptionMasterclasses)
    )
    Ok(views.html.event.masterclass(
      EventPortfolio(Nil, masterclassEvents.getEventsTagged(tag), None),
      pageInfo,
      MasterclassEvent.decodeTag(rawTag),
      MasterclassEvent.decodeTag(rawSubTag)
    ))
  }

  def list = CachedAction { implicit request =>
    val pageInfo = PageInfo(
      CopyConfig.copyTitleEvents,
      request.path,
      Some(CopyConfig.copyDescriptionEvents)
    )
    Ok(views.html.event.guardianLive(guLiveEvents.getEventPortfolio, pageInfo))
  }

  def listFilteredBy(urlTagText: String) = CachedAction { implicit request =>
    val tag = urlTagText.replace('-', ' ')
    val pageInfo = PageInfo(
      CopyConfig.copyTitleEvents,
      request.path,
      Some(CopyConfig.copyDescriptionEvents)
    )
    Ok(views.html.event.guardianLive(EventPortfolio(Seq.empty, guLiveEvents.getEventsTagged(tag), None), pageInfo))
  }

  def buy(id: String) = BuyAction(id).async { implicit request =>
    EventbriteService.getEvent(id).map { event =>
      if (memberCanBuyTicket(event, request.member)) redirectToEventbrite(request, event)
      else Future.successful(Redirect(routes.TierController.change()))
    }.getOrElse(Future.successful(NotFound))
  }

  private def memberCanBuyTicket(event: Eventbrite.EBEvent, member: Member): Boolean =
    event.generalReleaseTicket.exists { ticket =>
      TicketSaleDates.datesFor(event, ticket).tierCanBuyTicket(member.tier)
    }

  private def eventCookie(event: RichEvent) = s"mem-event-${event.id}"

  private def redirectToEventbrite(request: AnyMemberTierRequest[AnyContent], event: RichEvent): Future[Result] =
    Timing.record(metrics(event), "user-sent-to-eventbrite") {
      for {
        discountOpt <- memberService.createDiscountForMember(request.member, event)
      } yield Found(event.url ? ("discount" -> discountOpt.map(_.code)))
        .withCookies(Cookie(eventCookie(event), ""))
    }

  // log a conversion if the user came from a membership event page
  private def trackConversionToThankyou(request: Request[_], event: RichEvent) {
    request.cookies.get(eventCookie(event)).foreach { _ =>
      metrics(event).put("user-returned-to-thankyou-page", 1)
      println("tracked event")
    }
  }

  def thankyou(id: String, orderIdOpt: Option[String]) = MemberAction.async { implicit request =>
    orderIdOpt.fold {
      val resultOpt = for {
        oid <- request.flash.get("oid")
        event <- guLiveEvents.getBookableEvent(id)
      } yield {
        guLiveEvents.getOrder(oid).map { order =>
          trackConversionToThankyou(request, event)
          Ok(views.html.event.thankyou(event, order)).discardingCookies(DiscardingCookie(eventCookie(event)))
        }
      }
      resultOpt.getOrElse(Future.successful(NotFound))
    } { orderId =>
      Future.successful(Redirect(routes.Event.thankyou(id, None)).flashing("oid" -> orderId))
    }
  }

  def thankyouPixel(id: String) = NoCacheAction { implicit request =>
    EventbriteService.getEvent(id).map { event =>
      trackConversionToThankyou(request, event)
      NoContent.discardingCookies(DiscardingCookie(eventCookie(event)))
    }.getOrElse(NotFound)
  }
}

object Event extends Event {
  val guLiveEvents = GuardianLiveEventService
  val masterclassEvents = MasterclassEventService
  val memberService = MemberService
}
