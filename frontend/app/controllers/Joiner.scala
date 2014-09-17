package controllers

import model.{StatusFields, PrivateFields, PageInfo}
import play.api.Logger

import scala.concurrent.Future

import play.api.mvc.{Request, Controller}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import com.gu.membership.salesforce.Tier

import com.netaporter.uri.dsl._

import actions.{AnyMemberTierRequest, AuthRequest}
import services._
import forms.MemberForm.{FriendJoinForm, friendJoinForm}
import model.Eventbrite.{EBDiscount, EBEvent}
import configuration.{Config, CopyConfig}

trait Joiner extends Controller {

  val memberService: MemberService
  val eventService: EventbriteService

  def getEbIFrameDetail(request: AnyMemberTierRequest[_]): Future[Option[(String, Int)]] = {
    def getEbEventFromSession(request: Request[_]): Option[EBEvent] =
      PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(request).flatMap(eventService.getEvent)

    def detailsFor(event: EBEvent, discountOpt: Option[EBDiscount]): (String, Int) = {
      val url = (Config.eventbriteApiIframeUrl ? ("eid" -> event.id) & ("discount" -> discountOpt.map(_.code))).toString
      (url, event.ticket_classes.length)
    }

    Future.sequence {
      (for (event <- getEbEventFromSession(request)) yield {
        for (discountOpt <- memberService.createDiscountForMember(request.member, event)) yield detailsFor(event, discountOpt)
      }).toSeq
    }.map(_.headOption)
  }

  def tierList = CachedAction { implicit request =>
    val pageInfo = PageInfo(
      CopyConfig.copyTitleJoin,
      request.path,
      Some(CopyConfig.copyDescriptionJoin)
    )
    Ok(views.html.joiner.tierList(pageInfo))
  }

  def enterDetails(tier: Tier.Tier) = AuthenticatedNonMemberAction.async { implicit request =>
    val identityRequest = IdentityRequest(request)
    for {
      userOpt <- IdentityService.getFullUserDetails(request.user, identityRequest)
      privateFields = userOpt.map(_.privateFields).getOrElse(PrivateFields.apply())
      marketingChoices = userOpt.map(_.statusFields).getOrElse(StatusFields.apply())
      passwordExists <- IdentityService.doesUserPasswordExist(identityRequest)
    } yield {
      tier match {
        case Tier.Friend => Ok(views.html.joiner.detail.addressForm(privateFields, marketingChoices, passwordExists))
        case paidTier => Ok(views.html.joiner.payment.paymentForm(paidTier, privateFields, marketingChoices, passwordExists))
      }

    }
  }

  def joinFriend() = AuthenticatedNonMemberAction.async { implicit request =>
    friendJoinForm.bindFromRequest.fold(_ => Future.successful(BadRequest), makeFriend)
  }

  private def makeFriend(formData: FriendJoinForm)(implicit request: AuthRequest[_]) = {
    for {
      salesforceContactId <- MemberService.createMember(request.user, formData, IdentityRequest(request))
    } yield Redirect(routes.Joiner.thankyouFriend())
  }

  def patron() = CachedAction { implicit request =>
    val pageInfo = PageInfo(
      CopyConfig.copyTitlePatrons,
      request.path,
      Some(CopyConfig.copyDescriptionPatrons)
    )
    Ok(views.html.joiner.tier.patron(pageInfo))
  }

  def thankyouFriend() = MemberAction.async { implicit request =>
    for (eventbriteFrameDetail <- getEbIFrameDetail(request)) yield {
      Ok(views.html.joiner.thankyou.friend(eventbriteFrameDetail))
    }
  }

  def thankyouPaid(tier: Tier.Tier) = PaidMemberAction.async { implicit request =>
    for {
      customer <- StripeService.Customer.read(request.member.stripeCustomerId)
      subscriptionDetails <- SubscriptionService.getCurrentSubscriptionDetails(request.member.salesforceAccountId)
      eventbriteFrameDetail <- getEbIFrameDetail(request)
    } yield {
      Ok(views.html.joiner.thankyou.paid(customer.card, subscriptionDetails, eventbriteFrameDetail))
    }
  }

}

object Joiner extends Joiner {
  val memberService = MemberService
  val eventService = EventbriteService
}
