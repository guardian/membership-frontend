package controllers

import actions.{AuthenticatedAction, PaidMemberAction}
import com.gu.membership.salesforce.Tier
import configuration.Config
import controllers.Joining._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc.{Request, Controller}
import services.{MemberService, StripeService, EventbriteService, MemberRepository}
import model.Eventbrite._

import scala.concurrent.Future
import com.netaporter.uri.dsl._

trait Joiner extends Controller {

  val memberService: MemberService
  val eventService: EventbriteService

  def getEbEventFromSession(request: Request[_]): Future[Option[EBEvent]] = {
    val eventIdOpt = services.PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(request)
    Future.sequence(eventIdOpt.map(eventService.getEvent).toSeq).map(_.headOption)
  }

  def getEbIframeUrl(eventOpt: Option[EBEvent], discountOpt: Option[EBDiscount]): Option[String] = {
    for (event <- eventOpt) yield {
      Config.eventbriteApiIframeUrl ? ("eid" -> event.id) & ("discount" -> discountOpt.map(_.code))
    }
  }

  def tierList = CachedAction { implicit request =>
    Ok(views.html.joiner.tierList())
  }

  def enterDetails(tier: Tier.Tier) = AuthenticatedAction { implicit request =>
    tier match {
      case Tier.Friend => Ok(views.html.joiner.detail.addressForm())
      case paidTier => Ok(views.html.joiner.payment.paymentForm(paidTier))
    }
  }

  def joinFriend() = AuthenticatedAction.async { implicit request =>
    for {
      member <- MemberRepository.upsert(request.user, "", Tier.Friend)
    } yield Redirect(routes.Joiner.thankyouFriend())
  }

  def patron() = CachedAction { implicit request =>
    Ok(views.html.joiner.tier.patron())
  }

  def thankyouFriend() = AuthenticatedAction.async { implicit request =>

    for (event <- getEbEventFromSession(request)) yield {
      Ok(views.html.joiner.thankyou.friend(getEbIframeUrl(event, None)))
    }
  }

  def thankyouPaid(tier: Tier.Tier) = PaidMemberAction.async { implicit request =>

    def getDiscount(eventOpt: Option[EBEvent]): Future[Option[EBDiscount]] = {
      val discountOpt = eventOpt.map(memberService.createEventDiscount(request.user.id, _))
      Future.sequence(discountOpt.toSeq).map(_.headOption.flatten)
    }

    for {
      customer <- StripeService.Customer.read(request.stripeCustomerId)
      event <- getEbEventFromSession(request)
      discountOpt <- getDiscount(event.headOption)
    } yield {
      val response = for {
        paymentDetails <- customer.paymentDetails
      } yield Ok(views.html.joiner.thankyou.partner(paymentDetails, getEbIframeUrl(event.headOption, discountOpt)))

      response.getOrElse(NotFound)
    }
  }

}

object Joiner extends Joiner {
  val memberService = MemberService
  val eventService = EventbriteService
}
