package controllers

import scala.concurrent.Future

import com.netaporter.uri.dsl._

import play.api.mvc.{Result, Request, Controller}
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import com.gu.membership.salesforce.{ScalaforceError, Tier}

import actions.{AnyMemberTierRequest, AuthRequest}
import configuration.{Config, CopyConfig}
import forms.MemberForm.{friendJoinForm, paidMemberJoinForm, JoinForm}
import model._
import model.StripeSerializer._
import model.Eventbrite.{EBDiscount, EBEvent}
import services._

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
      privateFields = userOpt.fold(PrivateFields())(_.privateFields)
      marketingChoices = userOpt.fold(StatusFields())(_.statusFields)
      passwordExists <- IdentityService.doesUserPasswordExist(identityRequest)
    } yield {
      tier match {
        case Tier.Friend => Ok(views.html.joiner.detail.addressForm(privateFields, marketingChoices, passwordExists))
        case paidTier => Ok(views.html.joiner.payment.paymentForm(paidTier, privateFields, marketingChoices, passwordExists))
      }

    }
  }

  def joinFriend() = AuthenticatedNonMemberAction.async { implicit request =>
    friendJoinForm.bindFromRequest.fold(_ => Future.successful(BadRequest),
      makeMember { Redirect(routes.Joiner.thankyouFriend()) } )
  }

  def joinPaid(tier: Tier.Tier) = AuthenticatedNonMemberAction.async { implicit request =>
    paidMemberJoinForm.bindFromRequest.fold(_ => Future.successful(BadRequest),
      makeMember { Ok(Json.obj("redirect" -> routes.Joiner.thankyouPaid(tier).url)) } )
  }

  private def makeMember(result: Result)(formData: JoinForm)(implicit request: AuthRequest[_]) = {
    MemberService.createMember(request.user, formData, IdentityRequest(request))
      .map { _ => result }
      .recover {
        case error: Stripe.Error => Forbidden(Json.toJson(error))
        case error: Zuora.ResultError => Forbidden
        case error: ScalaforceError => Forbidden
      }
  }

  def thankyouFriend() = MemberAction.async { implicit request =>
    for {
      subscriptionDetails <- SubscriptionService.getCurrentSubscriptionDetails(request.member.salesforceAccountId)
      eventbriteFrameDetail <- getEbIFrameDetail(request)
    } yield {
      val event = PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(request).flatMap(eventService.getEvent)
      Ok(views.html.joiner.thankyou.friend(subscriptionDetails, eventbriteFrameDetail, request.member.firstName.getOrElse(""), request.user.primaryEmailAddress, event))
    }
  }

  def thankyouPaid(tier: Tier.Tier) = PaidMemberAction.async { implicit request =>
    for {
      customer <- StripeService.Customer.read(request.member.stripeCustomerId)
      subscriptionDetails <- SubscriptionService.getCurrentSubscriptionDetails(request.member.salesforceAccountId)
      eventbriteFrameDetail <- getEbIFrameDetail(request)
    } yield {
      val event = PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(request).flatMap(eventService.getEvent)
      Ok(views.html.joiner.thankyou.paid(customer.card, subscriptionDetails, eventbriteFrameDetail, tier, request.member.firstName.getOrElse(""), request.user.primaryEmailAddress, event))
    }
  }
}

object Joiner extends Joiner {
  val memberService = MemberService
  val eventService = EventbriteService
}
