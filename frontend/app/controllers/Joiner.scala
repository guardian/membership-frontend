package controllers

import abtests.ABTest
import actions.ActionRefiners._
import actions.Fallbacks.chooseRegister
import actions.{RichAuthRequest, _}
import com.github.nscala_time.time.Imports._
import com.gu.contentapi.client.model.v1.{MembershipTier => ContentAccess}
import com.gu.i18n.CountryGroup
import com.gu.i18n.CountryGroup.UK
import com.gu.i18n.Currency.GBP
import com.gu.identity.play.{AuthenticationService => _, _}
import com.gu.salesforce._
import com.gu.stripe.Stripe
import com.gu.stripe.Stripe.Serializer._
import com.gu.zuora.soap.models.errors._
import com.netaporter.uri.dsl._
import com.typesafe.scalalogging.LazyLogging
import configuration.{Config, CopyConfig}
import forms.MemberForm.{paidMemberJoinForm, _}
import model._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc._
import services.AuthenticationService.authenticatedIdUserProvider
import services.checkout.identitystrategy.Strategy.identityStrategyFor
import services.{GuardianContentService, _}
import tracking.ActivityTracking
import utils.RequestCountry._
import utils.TestUsers.{NameEnteredInForm, PreSigninTestCookie, isTestUser}
import utils.{CampaignCode, Feature, TierChangeCookies}
import views.support
import views.support.MembershipCompat._
import views.support.Pricing._
import views.support.TierPlans._
import views.support.{CheckoutForm, CountryWithCurrency, IdentityUser, PageInfo}

import scala.concurrent.Future
import scala.util.Failure

object Joiner extends Controller with ActivityTracking with PaymentGatewayErrorHandler
  with LazyLogging
  with CatalogProvider
  with StripeServiceProvider
  with SalesforceServiceProvider
  with SubscriptionServiceProvider
  with PaymentServiceProvider
  with MemberServiceProvider
  with ZuoraRestServiceProvider {
  val JoinReferrer = "join-referrer"

  val contentApiService = GuardianContentService

  val subscriberOfferDelayPeriod = 6.months

  val ForcedRedirectToIdentity = "forcedRedirectToIdentity"

  val EmailMatchingGuardianAuthenticatedStaffNonMemberAction = AuthenticatedStaffNonMemberAction andThen matchingGuardianEmail()

  val identityService = IdentityService(IdentityApi)

  def tierChooser = NoCacheAction { implicit request =>
    val eventOpt = PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(request.session).flatMap(EventbriteService.getBookableEvent)
    val accessOpt = request.getQueryString("membershipAccess").flatMap(ContentAccess.valueOf)
    val contentRefererOpt = request.headers.get(REFERER)

    val signInUrl = contentRefererOpt.map { referer =>
      ((Config.idWebAppUrl / "signin") ? ("returnUrl" -> referer) ? ("skipConfirmation" -> "true")).toString
    }.getOrElse(Config.idWebAppSigninUrl(""))

    implicit val countryGroup = UK
    val pageInfo = PageInfo(
      title = CopyConfig.copyTitleChooseTier,
      url = request.path,
      description = Some(CopyConfig.copyDescriptionChooseTier),
      customSignInUrl = Some(signInUrl)
    )

    Ok(views.html.joiner.tierChooser(TouchpointBackend.Normal.catalog, pageInfo, eventOpt, accessOpt, signInUrl))
      .withSession(request.session.copy(data = request.session.data ++ contentRefererOpt.map(JoinReferrer -> _)))
  }

  def staff = PermanentStaffNonMemberAction.async { implicit request =>
    val flashMsgOpt = request.flash.get("error").map(FlashMessage.error)
    val userSignedIn = AuthenticationService.authenticatedUserFor(request)
    val catalog = TouchpointBackend.Normal.catalog
    implicit val countryGroup = UK

    userSignedIn match {
      case Some(user) => for {
        fullUser <- IdentityService(IdentityApi).getFullUserDetails(user)(IdentityRequest(request))
        primaryEmailAddress = fullUser.primaryEmailAddress
        displayName = fullUser.publicFields.displayName
        avatarUrl = fullUser.privateFields.flatMap(_.socialAvatarUrl)
      } yield
        Ok(views.html.joiner.staff(catalog, StaffEmails(request.user.email, Some(primaryEmailAddress)), displayName, avatarUrl, flashMsgOpt))

      case _ =>
        Future.successful(
          Ok(views.html.joiner.staff(catalog, StaffEmails(request.user.email, None), None, None, flashMsgOpt)))
    }
  }

  def authenticatedIfNotMergingRegistration(onUnauthenticated: RequestHeader => Result = chooseRegister(_)) = new ActionFilter[Request] {
    override def filter[A](req: Request[A]) = Future.successful {
      val userOpt = authenticatedIdUserProvider(req)
      val userSignedIn = userOpt.isDefined
      val canWaiveAuth = abtests.MergedRegistration.allocate(req).exists(_.canWaiveAuth) || Feature.MergedRegistration.turnedOnFor(req)
      val canAccess = userSignedIn || canWaiveAuth
      logger.info(s"optional-auth ${req.path} canWaiveAuth=$canWaiveAuth userSignedIn=$userSignedIn canAccess=$canAccess testUser=${isTestUser(PreSigninTestCookie, req.cookies)(req).isDefined}")
      if (canAccess) None else Some(onUnauthenticated(req).addingToSession(ForcedRedirectToIdentity -> "true")(req))
    }
  }

  def OptionallyAuthenticatedNonMemberAction(tier: Tier) =
    NoCacheAction andThen PlannedOutageProtection andThen authenticatedIfNotMergingRegistration() andThen noAuthenticatedMemberFilter(onMember = redirectMemberAttemptingToSignUp(tier))

  def NonMemberAction(tier: Tier) =
    NoCacheAction andThen PlannedOutageProtection andThen authenticated() andThen onlyNonMemberFilter(onMember = redirectMemberAttemptingToSignUp(tier))

  def enterPaidDetails(
    tier: PaidTier,
    countryGroup: CountryGroup) = OptionallyAuthenticatedNonMemberAction(tier).async { implicit request =>
    val userOpt = authenticatedIdUserProvider(request)
    implicit val resolution: TouchpointBackend.Resolution =
      TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)

    implicit val tpBackend = resolution.backend

    implicit val backendProvider: BackendProvider = new BackendProvider {
      override def touchpointBackend = tpBackend
    }
    implicit val c = catalog

    val identityRequest = IdentityRequest(request)

    (for {
      identityUserOpt <- userOpt.map(user => identityService.getIdentityUserView(user, identityRequest).map(Option(_))).getOrElse(Future.successful[Option[IdentityUser]](None))
    } yield {
      tier match {
        case t: Tier.Supporter => for (user <- userOpt) {
          MembersDataAPI.Service.upsertBehaviour(
            user,
            activity = Some("enterPaidDetails.show"),
            note = Some(t.name))
        }
        case _ =>
      }
      val plans = catalog.findPaid(tier)
      val supportedCurrencies = plans.allPricing.map(_.currency).toSet
      val pageInfo = PageInfo(
        stripePublicKey = Some(stripeService.publicKey),
        payPalEnvironment = Some(tpBackend.payPalService.config.payPalEnvironment),
        initialCheckoutForm = CheckoutForm.forIdentityUser(identityUserOpt.flatMap(_.country), plans, Some(countryGroup)),
        abTests = abtests.ABTest.allocations(request)
      )

      val countryCurrencyWhitelist = CountryWithCurrency.whitelisted(supportedCurrencies, GBP)

      Ok(views.html.joiner.form.payment(
        plans,
        countryCurrencyWhitelist,
        identityUserOpt,
        pageInfo,
        Some(countryGroup),
        resolution))
    }).andThen { case Failure(e) => logger.error(s"User ${userOpt.map(_.id)} could not enter details for paid tier ${tier.name}: ${identityRequest.trackingParameters}", e)}
  }

  def enterFriendDetails = NonMemberAction(Tier.friend).async { implicit request =>
    implicit val backendProvider: BackendProvider = request
    implicit val c = catalog

    for {
      identityUser <- identityService.getIdentityUserView(request.user, IdentityRequest(request))
    } yield {

      val pageInfo = support.PageInfo(initialCheckoutForm = CheckoutForm.forIdentityUser(identityUser.country, catalog.friend, None))

      Ok(views.html.joiner.form.friendSignup(
        catalog.friend,
        identityUser,
        pageInfo))
    }
  }

  def enterStaffDetails = EmailMatchingGuardianAuthenticatedStaffNonMemberAction.async { implicit request =>
    val flashMsgOpt = request.flash.get("success").map(FlashMessage.success)
    implicit val backendProvider: BackendProvider = request
    for {
      identityUser <- identityService.getIdentityUserView(request.identityUser, IdentityRequest(request))
    } yield {
      Ok(views.html.joiner.form.addressWithWelcomePack(catalog.staff, identityUser, flashMsgOpt))
    }
  }

  def joinFriend = AuthenticatedNonMemberAction.async { implicit request =>
    friendJoinForm.bindFromRequest.fold(redirectToUnsupportedBrowserInfo,
      makeMember(Tier.friend, Redirect(routes.Joiner.thankyou(Tier.friend))))
  }

  def joinStaff = AuthenticatedNonMemberAction.async { implicit request =>
    staffJoinForm.bindFromRequest.fold(redirectToUnsupportedBrowserInfo,
      makeMember(Tier.partner, Redirect(routes.Joiner.thankyouStaff())))
  }

  def joinPaid(tier: PaidTier) = OptionallyAuthenticatedNonMemberAction(tier).async { implicit request =>
    paidMemberJoinForm.bindFromRequest.fold({ formWithErrors =>
      Future.successful(BadRequest(formWithErrors.errorsAsJson))
    },
      makeMember(tier, Ok(Json.obj("redirect" -> routes.Joiner.thankyou(tier).url))))
  }

  def updateEmailStaff() = AuthenticatedStaffNonMemberAction.async { implicit request =>
    val googleEmail = request.googleUser.email
    for {
      emailUpdateResult <- identityService.updateEmail(request.identityUser, googleEmail)(IdentityRequest(request)).value
    } yield emailUpdateResult.fold(
      _ => Redirect(routes.Joiner.staff()).flashing("error" ->
        s"There has been an error in updating your email. You may already have an Identity account with $googleEmail. Please try signing in with that email."),
      _ => Redirect(routes.Joiner.enterStaffDetails()).flashing("success" ->
        s"Your email address has been changed to $googleEmail"))
  }

  def unsupportedBrowser = CachedAction(Ok(views.html.joiner.unsupportedBrowser()))

  private def makeMember(tier: Tier, onSuccess: => Result)(formData: JoinForm)(implicit request: Request[_]) = {
    val userOpt = authenticatedIdUserProvider(request)
    logger.info(s"${s"User id=${userOpt.map(_.id).mkString}"} attempting to become ${tier.name}...")
    val eventId = PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(request.session)
    implicit val resolution: TouchpointBackend.Resolution =
      TouchpointBackend.forRequest(NameEnteredInForm, Some(formData))

    implicit val tpBackend = resolution.backend
    implicit val backendProvider: BackendProvider = new BackendProvider {
      override def touchpointBackend = tpBackend
    }
    salesforceService.metrics.putAttemptedSignUp(tier)
    val campaignCode = CampaignCode.fromRequest
    val ipCountry = request.getFastlyCountry

    val identityStrategy = identityStrategyFor(request, formData)
    identityStrategy.ensureIdUser { user =>
      memberService.createMember(user, formData, eventId, campaignCode, tier, ipCountry).map {
        case (sfContactId, zuoraSubName) =>
          logger.info(s"make-member-success ${tier.name} ${abtests.MergedRegistration.describeParticipation} ${Feature.MergedRegistration.describeState} ${identityStrategy.getClass.getSimpleName} user=${user.id} testUser=${isTestUser(user.minimal)} $ForcedRedirectToIdentity=${request.session.get(ForcedRedirectToIdentity).mkString} sub=$zuoraSubName")
          salesforceService.metrics.putSignUp(tier)
          trackRegistration(formData, tier, sfContactId, user.minimal, campaignCode)
          trackRegistrationViaEvent(sfContactId, user.minimal, eventId, campaignCode, tier)
          onSuccess
      }.recover {
        // errors due to user's card are logged at WARN level as they are not logic errors
        case error: Stripe.Error =>
          salesforceService.metrics.putFailSignUpStripe(tier)
          logger.warn(s"Stripe API call returned error: \n\t$error \n\tuser=$userOpt")
          setBehaviourNote(tier.name, error.code, userOpt)
          Forbidden(Json.toJson(error))

        case error: PaymentGatewayError =>
          salesforceService.metrics.putFailSignUpGatewayError(tier)
          setBehaviourNote(tier.name, error.code, userOpt)
          handlePaymentGatewayError(error, user.id, tier.name, formData.deliveryAddress.countryName)

        case error =>
          salesforceService.metrics.putFailSignUp(tier)
          logger.error(s"${s"User id=${userOpt.map(_.id).mkString}"} could not become ${tier.name} member", error)
          setBehaviourNote(tier.name, "card_error", userOpt)
          Forbidden
      }
    }
  }

  def thankyou(tier: Tier, upgrade: Boolean = false) = SubscriptionAction.async { implicit request =>
    implicit val resolution: TouchpointBackend.Resolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
    implicit val idReq = IdentityRequest(request)

    val emailFromZuora = zuoraRestService.getAccount(request.subscriber.subscription.accountId) map { account =>
      account.toOption.flatMap(_.billToContact.email)
    }

    for {
      paymentSummary <- memberService.getMembershipSubscriptionSummary(request.subscriber.contact)
      destination <- request.touchpointBackend.destinationService.returnDestinationFor(request.session, request.subscriber)
      paymentMethod <- paymentService.getPaymentMethod(request.subscriber.subscription.accountId)
      email <- emailFromZuora
    } yield {
      tier match {
        case t: Tier.Supporter if !upgrade => {salesforceService.metrics.putThankYou(tier)
          MembersDataAPI.Service.removeBehaviour(request.user)}
        case t: Tier if !upgrade => salesforceService.metrics.putThankYou(tier)
        case _ =>
      }
      Ok({salesforceService.metrics.putThankYou(tier)
        views.html.joiner.thankyou(
        request.subscriber,
        paymentSummary,
        paymentMethod,
        destination,
        upgrade,
        resolution,
        email
      )}).discardingCookies(TierChangeCookies.deletionCookies: _*)
    }
  }

  def thankyouStaff = thankyou(Tier.partner)

  private def setBehaviourNote(tier: String, errorCode: String, userOpt: Option[AuthenticatedIdUser]) = for (user <- userOpt) {
    if (tier.toLowerCase == "supporter") {
      MembersDataAPI.Service.upsertBehaviour(user, note = Some(errorCode))
    }
  }

}
