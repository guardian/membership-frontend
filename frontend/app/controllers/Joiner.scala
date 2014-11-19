package controllers

import actions.Functions._
import com.gu.identity.model.User

import scala.concurrent.Future

import com.netaporter.uri.dsl._

import play.api.mvc.{Result, Request, Controller}
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import com.gu.membership.salesforce.{ScalaforceError, Tier}

import actions._
import configuration.{Config, CopyConfig}
import forms.MemberForm.{friendJoinForm, paidMemberJoinForm, JoinForm}
import model._
import model.StripeSerializer._
import model.Eventbrite.{RichEvent, EBCode, EBEvent}
import services._

trait Joiner extends Controller {

  val memberService: MemberService
  val eventService: EventbriteService

  val AuthenticatedJoinStaffAction = NoCacheAction andThen OAuthActions.AuthAction
  
  val AuthenticatedStaffEnterDetailsAction = NoCacheAction andThen OAuthActions.AuthAction andThen authenticated()

  def getEbIFrameDetail(request: AnyMemberTierRequest[_]): Future[Option[(String, Int)]] = {
    def getEbEventFromSession(request: Request[_]): Option[RichEvent] =
      PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(request).flatMap(eventService.getEvent)

    def detailsFor(event: RichEvent, discountOpt: Option[EBCode]): (String, Int) = {
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

  def staff = AuthenticatedJoinStaffAction { implicit request =>
    Ok(views.html.joiner.staff())
  }

  def enterDetails(tier: Tier.Tier) = AuthenticatedNonMemberAction.async { implicit request =>
    val identityRequest = IdentityRequest(request)
    for {
      (privateFields, marketingChoices, passwordExists) <- identityDetails(request.user, identityRequest)
    } yield {

      tier match {
        case Tier.Friend => Ok(views.html.joiner.detail.addressForm(privateFields, marketingChoices, passwordExists))
        case paidTier =>
          val pageInfo = PageInfo.default.copy(stripePublicKey = Some(request.touchpointBackend.stripeService.publicKey))
          Ok(views.html.joiner.payment.paymentForm(paidTier, privateFields, marketingChoices, passwordExists, pageInfo))
      }

    }
  }

  def enterStaffDetails = AuthenticatedStaffEnterDetailsAction.async { implicit request =>
    val identityRequest = IdentityRequest(request)
    for {
      (privateFields, marketingChoices, passwordExists) <- identityDetails(request.user, identityRequest)
    } yield {
      Ok(views.html.joiner.detail.addressForm(privateFields, marketingChoices, passwordExists))
    }
  }

  private def identityDetails(user: com.gu.identity.model.User, identityRequest: IdentityRequest) = {
    for {
      userOpt <- IdentityService.getFullUserDetails(user, identityRequest)
      privateFields = userOpt.fold(PrivateFields())(_.privateFields)
      marketingChoices = userOpt.fold(StatusFields())(_.statusFields)
      passwordExists <- IdentityService.doesUserPasswordExist(identityRequest)
    } yield (privateFields, marketingChoices, passwordExists)
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
      subscriptionDetails <- request.touchpointBackend.subscriptionService.getCurrentSubscriptionDetails(request.member.salesforceAccountId)
      eventbriteFrameDetail <- getEbIFrameDetail(request)
    } yield {
      val event = PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(request).flatMap(eventService.getEvent)
      Ok(views.html.joiner.thankyou.friend(subscriptionDetails, eventbriteFrameDetail, request.member.firstName.getOrElse(""), request.user.primaryEmailAddress, event))
    }
  }

  def thankyouPaid(tier: Tier.Tier, upgrade: Boolean = false) = PaidMemberAction.async { implicit request =>
    for {
      customer <- request.touchpointBackend.stripeService.Customer.read(request.member.stripeCustomerId)
      subscriptionDetails <- request.touchpointBackend.subscriptionService.getCurrentSubscriptionDetails(request.member.salesforceAccountId)
      eventbriteFrameDetail <- getEbIFrameDetail(request)
    } yield {
      val event = PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(request).flatMap(eventService.getEvent)
      Ok(views.html.joiner.thankyou.paid(customer.card, subscriptionDetails, eventbriteFrameDetail, tier, request.member.firstName.getOrElse(""), request.user.primaryEmailAddress, event, upgrade))
    }
  }
}

object Joiner extends Joiner {
  val memberService = MemberService
  val eventService = GuardianLiveEventService
}
