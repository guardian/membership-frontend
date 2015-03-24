package controllers

import actions.Functions._
import actions._
import com.github.nscala_time.time.Imports._
import com.gu.membership.salesforce.{PaidMember, ScalaforceError, Tier}
import com.gu.membership.stripe.Stripe
import com.gu.membership.stripe.Stripe.Serializer._
import com.netaporter.uri.dsl._
import configuration.{Config, CopyConfig}
import forms.MemberForm._
import model.RichEvent._
import model._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc._
import services._
import services.EventbriteService._
import tracking.{ActivityTracking, EventActivity, EventData, MemberData}

import scala.concurrent.Future

trait Joiner extends Controller with ActivityTracking {
  val JoinReferrer = "join-referrer"

  val contentApiService = GuardianContentService

  val memberService: MemberService

  val EmailMatchingGuardianAuthenticatedStaffNonMemberAction = AuthenticatedStaffNonMemberAction andThen matchingGuardianEmail()

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

    val contentReferer = request.headers.get(REFERER)
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

  def enterDetails(tier: Tier) = AuthenticatedNonMemberWithKnownTierChangeAction(tier).async { implicit request =>
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

  def joinPaid(tier: Tier) = AuthenticatedNonMemberAction.async { implicit request =>
    paidMemberJoinForm.bindFromRequest.fold(_ => Future.successful(BadRequest),
      makeMember(tier, Ok(Json.obj("redirect" -> routes.Joiner.thankyou(tier).url))) )
  }

  private def makeMember(tier: Tier, result: Result)(formData: JoinForm)(implicit request: AuthRequest[_]) = {

    val useSubscriberOffer = formData match {
      case paidMemberJoinForm: PaidMemberJoinForm => paidMemberJoinForm.subscriberOffer
      case _ => false
    }

    //todo check user can use subscriber offer (again) before proceeding

    MemberService.createMember(request.user, formData, IdentityRequest(request), useSubscriberOffer)
      .map { member =>
        for {
          eventId <- PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(request)
          event <- EventbriteService.getBookableEvent(eventId)
        } {
          event.service.wsMetrics.put(s"join-$tier-event", 1)
          val memberData = MemberData(member.salesforceContactId, request.user.id, tier.name)
          track(EventActivity("membershipRegistrationViaEvent", Some(memberData), EventData(event)))(request.user)
        }
        result
      }.recover {
        case error: Stripe.Error => Forbidden(Json.toJson(error))
        case error: Zuora.ResultError => Forbidden
        case error: ScalaforceError => Forbidden
      }
  }

  def thankyou(tier: Tier, upgrade: Boolean = false) = MemberAction.async { implicit request =>
    val freeStartingPeriodOffer = true //todo this needs to come from zuora

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

    def getPaymentSummaryForMembershipsWithFreePeriod = {
      val subscriptionStatusFuture = request.touchpointBackend.subscriptionService.getSubscriptionStatus(request.member)

      for {
        subscriptionStatus <- subscriptionStatusFuture
        subscriptionDetails <- request.touchpointBackend.subscriptionService.getSubscriptionDetails(subscriptionStatus.current)
        paymentSummary <- request.touchpointBackend.subscriptionService.getPaymentSummaryWithFreeStartingPeriod(subscriptionStatus.current, true)
      } yield {
        val firstPreviewItem = paymentSummary.sortBy(_.serviceStartDate).headOption

        firstPreviewItem.map { firstPreviewInvoice =>

          MembershipSummary(subscriptionDetails.startDate, subscriptionDetails.endDate, 0f,
            subscriptionDetails.planAmount, Some(firstPreviewInvoice.price), firstPreviewInvoice.serviceStartDate)
        }
      }
    }

    def getPaymentSummaryForMembershipsNoFreePeriod = {
      for {
        paymentSummary <- request.touchpointBackend.subscriptionService.getPaymentSummary(request.member)

      } yield Some(MembershipSummary(paymentSummary.current.serviceStartDate, paymentSummary.current.serviceEndDate,
        paymentSummary.totalPrice, paymentSummary.current.price, None, paymentSummary.current.nextPaymentDate))
    }

    val futureContentOpt = request.session.get(JoinReferrer).map { referer =>
      contentApiService.contentItemQuery(referer.path).map(_.content.map(MembersOnlyContent))
    }.getOrElse(Future.successful(None))

    for {
      customerOpt <- futureCustomerOpt
      eventDetailsOpt <- futureEventDetailsOpt
      summary <- if(freeStartingPeriodOffer) getPaymentSummaryForMembershipsWithFreePeriod else getPaymentSummaryForMembershipsNoFreePeriod
      contentOpt <- futureContentOpt.recover { case _ => None }
    } yield Ok(views.html.joiner.thankyou(
        request.member,
        summary, 
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