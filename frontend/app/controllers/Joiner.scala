package controllers

import actions.Functions._
import actions._
import com.github.nscala_time.time.Imports
import com.github.nscala_time.time.Imports._
import com.gu.cas.CAS.CASSuccess
import com.gu.identity.play.{IdMinimalUser, StatusFields}
import com.gu.membership.salesforce.{PaidMember, ScalaforceError, Tier}
import com.gu.membership.stripe.Stripe
import com.gu.membership.stripe.Stripe.Serializer._
import com.gu.membership.zuora.soap.models.errors.ResultError
import com.netaporter.uri.dsl._
import com.typesafe.scalalogging.LazyLogging
import configuration.{Config, CopyConfig, Email}
import forms.MemberForm._
import model._
import play.api.data.Form
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc._
import services.eventbrite.EventbriteService
import services.{GuardianContentService, _}
import EventbriteService._
import tracking.{ActivityTracking, EventActivity, EventData, MemberData}
import utils.CampaignCode.extractCampaignCode

import scala.concurrent.Future

trait Joiner extends Controller with ActivityTracking with LazyLogging {
  val JoinReferrer = "join-referrer"

  val contentApiService = GuardianContentService

  val memberService: MemberService

  val subscriberOfferDelayPeriod = 6.months

  val casService = CASService

  val EmailMatchingGuardianAuthenticatedStaffNonMemberAction = AuthenticatedStaffNonMemberAction andThen matchingGuardianEmail()

  def tierChooser = NoCacheAction { implicit request =>

    val eventOpt = PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(request).flatMap(EventbriteService.getBookableEvent)
    val accessOpt = request.getQueryString("membershipAccess").map(MembershipAccess)
    val contentRefererOpt = request.headers.get(REFERER)

    val signInUrl = contentRefererOpt.map { referer =>
      ((Config.idWebAppUrl / "signin") ? ("returnUrl" -> referer) ? ("skipConfirmation" -> "true")).toString
    }.getOrElse(Config.idWebAppSigninUrl(""))

    val pageInfo = PageInfo(
      title=CopyConfig.copyTitleChooseTier,
      url=request.path,
      description=Some(CopyConfig.copyDescriptionChooseTier),
      customSignInUrl=Some(signInUrl)
    )

    Ok(views.html.joiner.tierChooser(pageInfo, eventOpt, accessOpt, signInUrl))
      .withSession(request.session.copy(data = request.session.data ++ contentRefererOpt.map(JoinReferrer -> _)))

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

  def enterDetails(tier: Tier) = (AuthenticatedAction andThen onlyNonMemberFilter(onMember = redirectMemberAttemptingToSignUp(tier))).async { implicit request =>
    for {
      (privateFields, marketingChoices, passwordExists) <- identityDetails(request.user, request)
    } yield {

      tier match {
        case Tier.Friend => Ok(views.html.joiner.form.friendSignup(privateFields, marketingChoices, passwordExists))
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
    friendJoinForm.bindFromRequest.fold(redirectToUnsupportedBrowserInfo,
      makeMember(Tier.Friend, Redirect(routes.Joiner.thankyou(Tier.Friend))) )
  }

  def joinStaff() = AuthenticatedNonMemberAction.async { implicit request =>
    staffJoinForm.bindFromRequest.fold(redirectToUnsupportedBrowserInfo,
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

  def unsupportedBrowser = CachedAction(Ok(views.html.joiner.unsupportedBrowser()))

  def joinPaid(tier: Tier) = AuthenticatedNonMemberAction.async { implicit request =>
    paidMemberJoinForm.bindFromRequest.fold(redirectToUnsupportedBrowserInfo,
      makeMember(tier, Ok(Json.obj("redirect" -> routes.Joiner.thankyou(tier).url))) )
  }

  def redirectToUnsupportedBrowserInfo(form: Form[_])(implicit req: RequestHeader): Future[Result] = {
    logger.error(s"Server-side form errors on joining indicates a Javascript problem: ${req.headers.get(USER_AGENT)}")
    logger.debug(s"Server-side form errors : ${form.errors}")
    Future.successful(Redirect(routes.Joiner.unsupportedBrowser()))
  }

  private def makeMember(tier: Tier, result: Result)(formData: JoinForm)(implicit request: AuthRequest[_]) = {

    def checkCASIfRequiredAndReturnPaymentDelay(formData: JoinForm)(implicit request: AuthRequest[_]): Future[Either[String,Option[Period]]] = {
      formData match {
        case paidMemberJoinForm: PaidMemberJoinForm => {
          paidMemberJoinForm.casId map { casId =>
            for {
              casResult <- casService.check(casId, Some(formData.deliveryAddress.postCode), formData.name.last)
              casIdNotUsed <- request.touchpointBackend.subscriptionService.getSubscriptionsByCasId(casId)
            } yield {
              casResult match {
                case success: CASSuccess if new DateTime(success.expiryDate).isAfterNow && casIdNotUsed.isEmpty => Right(Some(subscriberOfferDelayPeriod))
                case _ => Left(s"Subscriber details invalid. Please contact ${Email.membershipSupport} for further assistance.")
              }
            }
          }
        }.getOrElse(Future.successful(Right(None)))
        case _ => Future.successful(Right(None))
      }
    }

    def makeMemberAfterValidation(subscriberValidation: Either[String, Option[Imports.Period]]) = {
      subscriberValidation.fold({ errorString: String =>
        Future.successful(Forbidden)
      },{ paymentDelayOpt: Option[Period] =>
        MemberService.createMember(request.user, formData, IdentityRequest(request), paymentDelayOpt, extractCampaignCode(request))
          .map { member =>
          for {
            eventId <- PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(request)
            event <- EventbriteService.getBookableEvent(eventId)
          } {
            event.service.wsMetrics.put(s"join-$tier-event", 1)
            val memberData = MemberData(member.salesforceContactId, request.user.id, tier.name, campaignCode=extractCampaignCode(request))
            track(EventActivity("membershipRegistrationViaEvent", Some(memberData), EventData(event)), request.user)
          }
          result
        }.recover {
          case error: Stripe.Error => Forbidden(Json.toJson(error))
      	  case error: ResultError => Forbidden
          case error: ScalaforceError => Forbidden
          case error: MemberServiceError => Forbidden
        }
      })
    }

    for {
      subscriberValidation <- checkCASIfRequiredAndReturnPaymentDelay(formData)
      member <- makeMemberAfterValidation(subscriberValidation)
    } yield member

  }

  def thankyou(tier: Tier, upgrade: Boolean = false) = MemberAction.async { implicit request =>

    def futureCustomerOpt = request.member match {
      case paidMember: PaidMember =>
        request.touchpointBackend.stripeService.Customer.read(paidMember.stripeCustomerId).map(Some(_))
      case _ => Future.successful(None)
    }

    for {
      paymentSummary <- request.touchpointBackend.subscriptionService.getMembershipSubscriptionSummary(request.member)
      customerOpt <- futureCustomerOpt
      destinationOpt <- DestinationService.returnDestinationFor(request)
    } yield Ok(views.html.joiner.thankyou(
        request.member,
        paymentSummary,
        customerOpt.map(_.card),
        destinationOpt,
        upgrade
    )).discardingCookies(DiscardingCookie("GU_MEM"))
  }

  def thankyouStaff = thankyou(Tier.Partner)
}

object Joiner extends Joiner {
  val memberService = MemberService
}
