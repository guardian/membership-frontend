package controllers

import scala.concurrent.Future

import play.api.mvc.{Request, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import com.gu.membership.salesforce.Tier

import com.netaporter.uri.dsl._

import actions.{AuthRequest, PaidMemberAction, AuthenticatedAction}
import services._
import forms.MemberForm.{FriendJoinForm, friendJoinForm}
import model.Eventbrite.{EBDiscount, EBEvent}
import configuration.Config

trait Joiner extends Controller {

  val memberService: MemberService
  val eventService: EventbriteService

  def getEbEventFromSession(request: Request[_]): Future[Option[EBEvent]] = {
    val eventIdOpt = services.PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(request)
    Future.sequence(eventIdOpt.map(eventService.getEvent).toSeq).map(_.headOption)
  }

  def getEbIFrameDetail(eventOpt: Option[EBEvent], discountOpt: Option[EBDiscount]): Option[(String, Int)] = {
    for (event <- eventOpt) yield {
      (Config.eventbriteApiIframeUrl ? ("eid" -> event.id) & ("discount" -> discountOpt.map(_.code))).toString ->
      event.ticket_classes.length
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
    friendJoinForm.bindFromRequest.fold(_ => Future.successful(BadRequest), makeFriend)
  }

  private def makeFriend(formData: FriendJoinForm)(implicit request: AuthRequest[_]) = {
    for {
      salesforceContactId <- MemberService.createFriend(request.user, formData, request.cookies.get("SC_GU_U"))
    } yield Redirect(routes.Joiner.thankyouFriend())
  }

  def patron() = CachedAction { implicit request =>
    Ok(views.html.joiner.tier.patron())
  }

  def thankyouFriend() = AuthenticatedAction.async { implicit request =>

    for (event <- getEbEventFromSession(request)) yield {
      Ok(views.html.joiner.thankyou.friend(getEbIFrameDetail(event, None)))
    }
  }

  def thankyouPaid(tier: Tier.Tier) = PaidMemberAction.async { implicit request =>

    def getDiscount(eventOpt: Option[EBEvent]): Future[Option[EBDiscount]] = {
      val discountOpt = eventOpt.map(memberService.createEventDiscount(request.user.id, _))
      Future.sequence(discountOpt.toSeq).map(_.headOption.flatten)
    }

    for {
      customer <- StripeService.Customer.read(request.member.stripeCustomerId)
      subscriptionDetails <- SubscriptionService.getCurrentSubscriptionDetails(request.member.salesforceAccountId)
      eventOpt <- getEbEventFromSession(request)
      discountOpt <- getDiscount(eventOpt)
    } yield {
      Ok(views.html.joiner.thankyou.paid(customer.card, subscriptionDetails, getEbIFrameDetail(eventOpt, discountOpt)))
    }
  }

}

object Joiner extends Joiner {
  val memberService = MemberService
  val eventService = EventbriteService
}
