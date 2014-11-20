package controllers

import actions.AnyMemberTierRequest
import com.gu.membership.salesforce.Member
import com.gu.membership.util.Timing
import model.Eventbrite.{RichEvent, EBEvent, MasterclassEvent}
import model.{TicketSaleDates, Eventbrite, EventPortfolio, PageInfo}
import monitoring.EventbriteMetrics
import org.joda.time.Instant

import scala.concurrent.Future

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import actions.Functions._
import actions.Fallbacks.notYetAMemberOn
import services.{MasterclassEventService, GuardianLiveEventService, MemberService, EventbriteService}
import configuration.{Config, CopyConfig}

import com.netaporter.uri.dsl._
import views.support.Dates._

trait Event extends Controller {
  val guLiveEvents: EventbriteService
  val masterclassEvents: EventbriteService

  val memberService: MemberService

  val BuyAction = NoCacheAction andThen metricRecord(EventbriteMetrics, "buy-action-invoked") andThen authenticated(onUnauthenticated = notYetAMemberOn(_)) andThen memberRefiner()

  private def getEvent(id: String) =
    guLiveEvents.getEvent(id) orElse masterclassEvents.getEvent(id)

  def details(id: String) = CachedAction { implicit request =>
    getEvent(id).map { event =>
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
      CopyConfig.copyTitleEvents,
      request.path,
      Some(CopyConfig.copyDescriptionEvents)
    )
    Ok(views.html.event.masterclass(masterclassEvents.getEventPortfolio, pageInfo, "All categories"))
  }

  def masterclassesByTag(rawTag: String) = CachedAction { implicit request =>
    val tag = MasterclassEvent.decodeTag(rawTag)
    val pageInfo = PageInfo(
      CopyConfig.copyTitleEvents,
      request.path,
      Some(CopyConfig.copyDescriptionEvents)
    )
    Ok(views.html.event.masterclass(EventPortfolio(Nil, masterclassEvents.getEventsTagged(tag)), pageInfo, tag))
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
    Ok(views.html.event.guardianLive(EventPortfolio(Seq.empty, guLiveEvents.getEventsTagged(tag)), pageInfo))
  }

  def buy(id: String) = BuyAction.async { implicit request =>
    getEvent(id).map { event =>
      if(memberCanBuyTicket(event, request.member)) redirectToEventbrite(request, event)
      else Future.successful(Redirect(routes.TierController.change()))
    }.getOrElse(Future.successful(NotFound))
  }

  private def memberCanBuyTicket(event: Eventbrite.EBEvent, member: Member): Boolean =
    event.generalReleaseTicket.exists { ticket =>
      TicketSaleDates.datesFor(event, ticket).tierCanBuyTicket(member.tier)
    }

  private def redirectToEventbrite(request: AnyMemberTierRequest[AnyContent], event: RichEvent): Future[Result] =
    Timing.record(EventbriteMetrics, "user-sent-to-eventbrite") {
      for {
        discountOpt <- memberService.createDiscountForMember(request.member, event)
      } yield Found(event.url ? ("discount" -> discountOpt.map(_.code)))
    }

  def thankyou(id: String, orderIdOpt: Option[String]) = MemberAction.async { implicit request =>
    orderIdOpt.fold {
      val resultOpt = for {
        oid <- request.flash.get("oid")
        event <- guLiveEvents.getEvent(id)
      } yield {
        guLiveEvents.getOrder(oid).map { order =>
          EventbriteMetrics.putThankyou(id)
          Ok(views.html.event.thankyou(event, order))
        }
      }

      resultOpt.getOrElse(Future.successful(NotFound))
    } { orderId =>
      Future.successful(Redirect(routes.Event.thankyou(id, None)).flashing("oid" -> orderId))
    }
  }
}

object Event extends Event {
  val guLiveEvents = GuardianLiveEventService
  val masterclassEvents = MasterclassEventService
  val memberService = MemberService
}
