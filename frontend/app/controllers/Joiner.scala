

package controllers

import actions.Functions._
import actions._
import com.gu.membership.salesforce.{PaidMember, ScalaforceError, Tier}
import com.gu.membership.stripe.Stripe
import com.gu.membership.stripe.Stripe.Serializer._
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import configuration.{Config, CopyConfig}
import controllers.Testing.AuthorisedTester
import forms.MemberForm.{JoinForm, friendJoinForm, paidMemberJoinForm, staffJoinForm}
import model.RichEvent._
import model._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc._
import services._
import services.GuardianContentService
import services.EventbriteService._

import scala.concurrent.Future
import tracking.{EventData, EventActivity, MemberData, ActivityTracking}

trait Joiner extends Controller with ActivityTracking {
  val JoinReferrer = "join-referrer"

  val contentApiService = GuardianContentService

  val memberService: MemberService

  val EmailMatchingGuardianAuthenticatedStaffNonMemberAction = AuthenticatedStaffNonMemberAction andThen matchingGuardianEmail()

  def secureHiddenTiers(tier: Tier) = if (Tier.allPublic.contains(tier)) Action else AuthorisedTester

  def tierList = CachedAction { implicit request =>
    val pageInfo = PageInfo(
      CopyConfig.copyTitleJoin,
      request.path,
      Some(CopyConfig.copyDescriptionJoin)
    )
    Ok(views.html.joiner.tierList(pageInfo))
  }

  def tierChooser = NoCacheAction { implicit request =>
    val eventOpt = PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(request).flatMap(EventbriteService.getBookableEvent)
    val pageInfo = PageInfo(
      CopyConfig.copyTitleChooseTier,
      request.path,
      Some(CopyConfig.copyDescriptionChooseTier)
    )

    val contentReferer = request.headers.get("referer")
    val contentAccess = request.getQueryString("membershipAccess")

    Ok(views.html.joiner.tierChooser(eventOpt, pageInfo)).withSession(request.session.copy(data = request.session.data ++ contentReferer.map(JoinReferrer -> _)))
  }

  def staff = PermanentStaffNonMemberAction.async { implicit request =>
    val flashMsgOpt = request.flash.get("error").map(FlashMessage.error)
    val userSignedIn = AuthenticationService.authenticatedUserFor(request)
    userSignedIn match {
      case Some(user) => for {
        fullUser <- IdentityService(IdentityApi).getFullUserDetails(user, IdentityRequest(request))
        primaryEmailAddress = fullUser.primaryEmailAddress
        displayName = fullUser.publicFields.displayName
        avatarUrl = fullUser.privateFields.socialAvatarUrl
      } yield {
        Ok(views.html.joiner.staff(new StaffEmails(request.user.email, Some(primaryEmailAddress)), displayName, avatarUrl, flashMsgOpt))
      }
      case _ => Future.successful(Ok(views.html.joiner.staff(new StaffEmails(request.user.email, None), None, None, flashMsgOpt)))
    }
  }

  def enterDetails(tier: Tier) = (secureHiddenTiers(tier) andThen AuthenticatedNonMemberWithKnownTierChangeAction(tier)).async { implicit request =>
    for {
      (privateFields, marketingChoices, passwordExists) <- identityDetails(request.user, request)
    } yield {

      tier match {
        case Tier.Friend => Ok(views.html.joiner.form.address(privateFields, marketingChoices, passwordExists))
        case paidTier =>
          val pageInfo = PageInfo.default.copy(stripePublicKey = Some(request.touchpointBackend.stripeService.publicKey))
          Ok(views.html.joiner.form.payment(paidTier, privateFields, marketingChoices, passwordExists, pageInfo))
      }

    }
  }

  def enterStaffDetails = EmailMatchingGuardianAuthenticatedStaffNonMemberAction.async { implicit request =>
    val flashMsgOpt = request.flash.get("success").map(FlashMessage.success)
    for {
      (privateFields, marketingChoices, passwordExists) <- identityDetails(request.identityUser, request)
    } yield {
      Ok(views.html.joiner.form.addressWithWelcomePack(privateFields, marketingChoices, passwordExists, flashMsgOpt))
    }
  }

  private def identityDetails(user: IdMinimalUser, request: Request[_]) = {
    val identityService = IdentityService(IdentityApi)
    val identityRequest = IdentityRequest(request)
    for {
      user <- identityService.getFullUserDetails(user, identityRequest)
      passwordExists <- identityService.doesUserPasswordExist(identityRequest)
    } yield (user.privateFields, user.statusFields.getOrElse(StatusFields()), passwordExists)
  }

  def joinFriend() = AuthenticatedNonMemberAction.async { implicit request =>
    friendJoinForm.bindFromRequest.fold(_ => Future.successful(BadRequest),
      makeMember(Tier.Friend, Redirect(routes.Joiner.thankyou(Tier.Friend))) )
  }

  def joinStaff() = AuthenticatedNonMemberAction.async { implicit request =>
    staffJoinForm.bindFromRequest.fold(_ => Future.successful(BadRequest),
        makeMember(Tier.Partner, Redirect(routes.Joiner.thankyouStaff())) )
  }

  def updateEmailStaff() = AuthenticatedStaffNonMemberAction.async { implicit request =>
    val googleEmail = request.googleUser.email
    for {
      responseCode <- IdentityService(IdentityApi).updateEmail(request.identityUser, googleEmail, IdentityRequest(request))
    }
    yield {
      responseCode match {
        case 200 => Redirect(routes.Joiner.enterStaffDetails())
                  .flashing("success" ->
          s"Your email address has been changed to ${googleEmail}")
        case _ => Redirect(routes.Joiner.staff())
                  .flashing("error" ->
          s"There has been an error in updating your email. You may already have an Identity account with ${googleEmail}. Please try signing in with that email.")
      }
    }
  }

  def joinPaid(tier: Tier) = (secureHiddenTiers(tier) andThen AuthenticatedNonMemberAction).async { implicit request =>
    paidMemberJoinForm.bindFromRequest.fold(_ => Future.successful(BadRequest),
      makeMember(tier, Ok(Json.obj("redirect" -> routes.Joiner.thankyou(tier).url))) )
  }

  private def makeMember(tier: Tier, result: Result)(formData: JoinForm)(implicit request: AuthRequest[_]) = {

    MemberService.createMember(request.user, formData, IdentityRequest(request))
      .map { member =>
        for {
          eventId <- PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(request)
          event <- EventbriteService.getBookableEvent(eventId)
        } {
          event.service.wsMetrics.put(s"join-$tier-event", 1)
          val memberData = MemberData(member.salesforceContactId, request.user.id, tier.name)
          track(EventActivity("membershipRegistrationViaEvent", Some(memberData), EventData(event)))
        }
        result
      }.recover {
        case error: Stripe.Error => Forbidden(Json.toJson(error))
        case error: Zuora.ResultError => Forbidden
        case error: ScalaforceError => Forbidden
      }
  }

  def thankyou(tier: Tier, upgrade: Boolean = false) = (secureHiddenTiers(tier) andThen MemberAction).async { implicit request =>

    def futureCustomerOpt = request.member match {
      case paidMember: PaidMember =>
        request.touchpointBackend.stripeService.Customer.read(paidMember.stripeCustomerId).map(Some(_))
      case _ => Future.successful(None)
    }

    def futureEventDetailsOpt = {
      val optFuture = for {
        eventId <- PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(request)
        event <- EventbriteService.getBookableEvent(eventId)
      } yield {

        MemberService.createDiscountForMember(request.member, event).map { discountOpt =>
          (event, (Config.eventbriteApiIframeUrl ? ("eid" -> event.id) & ("discount" -> discountOpt.map(_.code))).toString)
        }

      }

      Future.sequence(optFuture.toSeq).map(_.headOption)
    }

    val futureContentOpt = request.session.get(JoinReferrer).map { referer =>
      contentApiService.contentItemQuery(referer.path).map(_.content.map(MembersOnlyContent))
    }.getOrElse(Future.successful(None))

    for {
      paymentSummary <- request.touchpointBackend.subscriptionService.getPaymentSummary(request.member)
      customerOpt <- futureCustomerOpt
      eventDetailsOpt <- futureEventDetailsOpt
      contentOpt <- futureContentOpt
    } yield Ok(views.html.joiner.thankyou(
        request.member,
        paymentSummary,
        customerOpt.map(_.card),
        eventDetailsOpt,
        contentOpt,
        upgrade
    )).discardingCookies(DiscardingCookie("GU_MEM"))
  }

  def thankyouStaff = thankyou(Tier.Partner)
}

object Joiner extends Joiner {
  val memberService = MemberService
}
