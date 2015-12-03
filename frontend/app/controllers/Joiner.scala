package controllers

import actions.Functions._
import actions._
import com.github.nscala_time.time.Imports
import com.github.nscala_time.time.Imports._
import com.gu.cas.CAS.CASSuccess
import com.gu.identity.play.{PrivateFields, IdMinimalUser, StatusFields}
import com.gu.membership.model.GBP
import com.gu.membership.salesforce._
import com.gu.membership.stripe.Stripe
import com.gu.membership.stripe.Stripe.Serializer._
import com.gu.membership.zuora.soap.models.errors.{GatewayError, ResultError}
import com.netaporter.uri.dsl._
import com.typesafe.scalalogging.LazyLogging
import configuration.{Config, CopyConfig, Email}
import forms.MemberForm._
import model._
import play.api.data.Form
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc._
import services.{GuardianContentService, _}
import services.EventbriteService._
import tracking.{ActivityTracking, EventActivity, EventData, MemberData}
import utils.CampaignCode.extractCampaignCode
import utils.TierChangeCookies

import scala.concurrent.Future

trait Joiner extends Controller with ActivityTracking with LazyLogging {
  val JoinReferrer = "join-referrer"
  implicit val currency = GBP

  val contentApiService = GuardianContentService

  val memberService: MemberService

  val subscriberOfferDelayPeriod = 6.months

  val casService = CASService

  val EmailMatchingGuardianAuthenticatedStaffNonMemberAction = AuthenticatedStaffNonMemberAction andThen matchingGuardianEmail()

  def tierChooser = NoCacheAction.async { implicit request =>
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

    TouchpointBackend.Normal.catalog.map(cat =>
      Ok(views.html.joiner.tierChooser(cat, pageInfo, eventOpt, accessOpt, signInUrl))
        .withSession(request.session.copy(data = request.session.data ++ contentRefererOpt.map(JoinReferrer -> _)))
    )
  }

  def staff = PermanentStaffNonMemberAction.async { implicit request =>
    val flashMsgOpt = request.flash.get("error").map(FlashMessage.error)
    val userSignedIn = AuthenticationService.authenticatedUserFor(request)
    val catalogF = TouchpointBackend.Normal.catalog
    userSignedIn match {
      case Some(user) => for {
        fullUser <- IdentityService(IdentityApi).getFullUserDetails(user, IdentityRequest(request))
        catalog <- catalogF
        primaryEmailAddress = fullUser.primaryEmailAddress
        displayName = fullUser.publicFields.displayName
        avatarUrl = fullUser.privateFields.flatMap(_.socialAvatarUrl)
      } yield {
        Ok(views.html.joiner.staff(catalog, new StaffEmails(request.user.email, Some(primaryEmailAddress)), displayName, avatarUrl, flashMsgOpt))
      }
      case _ => catalogF.map(cat => Ok(views.html.joiner.staff(cat, new StaffEmails(request.user.email, None), None, None, flashMsgOpt)))
    }
  }

  def enterDetails(tier: Tier) = (AuthenticatedAction andThen onlyNonMemberFilter(onMember = redirectMemberAttemptingToSignUp(tier))).async { implicit request =>
    for {
      (privateFields, marketingChoices, passwordExists) <- identityDetails(request.user, request)
      catalog <- request.catalog
    } yield {
      catalog.publicTierDetails(tier) match {
        case freeDetails: FreeTierDetails => Ok(views.html.joiner.form.friendSignup(freeDetails, getOrBlank(privateFields), marketingChoices, passwordExists))
        case paidDetails: PaidTierDetails =>
          val pageInfo = PageInfo.default.copy(stripePublicKey = Some(request.touchpointBackend.stripeService.publicKey))
          Ok(views.html.joiner.form.payment(paidDetails, getOrBlank(privateFields), marketingChoices, passwordExists, pageInfo))
      }
    }
  }

  def enterStaffDetails = EmailMatchingGuardianAuthenticatedStaffNonMemberAction.async { implicit request =>
    val flashMsgOpt = request.flash.get("success").map(FlashMessage.success)
    for {
      (privateFields, marketingChoices, passwordExists) <- identityDetails(request.identityUser, request)
    } yield {
      Ok(views.html.joiner.form.addressWithWelcomePack(getOrBlank(privateFields), marketingChoices, passwordExists, flashMsgOpt))
    }
  }

  private def getOrBlank(privateFields: Option[PrivateFields]): PrivateFields =
    privateFields.getOrElse(PrivateFields())

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

  def joinPaid(tier: PaidTier) = AuthenticatedNonMemberAction.async { implicit request =>
    paidMemberJoinForm.bindFromRequest.fold(redirectToUnsupportedBrowserInfo,
      makeMember(tier, Ok(Json.obj("redirect" -> routes.Joiner.thankyou(tier).url))) )
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
          s"Your email address has been changed to $googleEmail")
        case _ => Redirect(routes.Joiner.staff())
                  .flashing("error" ->
          s"There has been an error in updating your email. You may already have an Identity account with $googleEmail. Please try signing in with that email.")
      }
    }
  }

  def unsupportedBrowser = CachedAction(Ok(views.html.joiner.unsupportedBrowser()))

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
          case GatewayError(_, _, "Declined") => Forbidden(Json.toJson(Stripe.Error("card_error", "", "card_declined")))
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

    def futureCustomerOpt = request.member.paymentMethod match {
      case StripePayment(id) =>
        request.touchpointBackend.stripeService.Customer.read(id).map(Some(_))
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
    )).discardingCookies(TierChangeCookies.deletionCookies:_*)
  }

  def thankyouStaff = thankyou(Tier.Partner)
}

object Joiner extends Joiner {
  val memberService = MemberService
}
