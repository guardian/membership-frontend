package controllers

import actions.ActionRefiners._
import actions.{RichAuthRequest, _}
import com.github.nscala_time.time.Imports._
import com.gu.contentapi.client.model.v1.{MembershipTier => ContentAccess}
import com.gu.i18n.CountryGroup
import com.gu.i18n.CountryGroup.UK
import com.gu.i18n.Currency.GBP
import com.gu.memsub.BillingPeriod
import com.gu.memsub.promo.{NewUsers, PromoCode}
import com.gu.memsub.util.Timing
import com.gu.salesforce._
import com.gu.stripe.Stripe
import com.gu.stripe.Stripe.Serializer._
import com.gu.zuora.soap.models.errors._
import com.netaporter.uri.Uri
import com.netaporter.uri.dsl._
import com.typesafe.scalalogging.LazyLogging
import configuration.{Config, CopyConfig}
import forms.MemberForm._
import model._
import play.api.Play.current
import play.api.i18n.Messages.Implicits._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.mvc._
import services.PromoSessionService.codeFromSession
import services.{GuardianContentService, _}
import tracking.ActivityTracking
import utils.RequestCountry._
import utils.TestUsers.{NameEnteredInForm, PreSigninTestCookie}
import utils.{CampaignCode, TierChangeCookies}
import views.support
import views.support.MembershipCompat._
import views.support.Pricing._
import views.support.TierPlans._
import views.support.{CheckoutForm, CountryWithCurrency, PageInfo}

import scala.concurrent.Future
import scala.util.Failure

object Joiner extends Controller with ActivityTracking
  with LazyLogging
  with CatalogProvider
  with StripeServiceProvider
  with SalesforceServiceProvider
  with SubscriptionServiceProvider
  with PromoServiceProvider
  with PaymentServiceProvider
  with MemberServiceProvider {
  val JoinReferrer = "join-referrer"

  val contentApiService = GuardianContentService

  val subscriberOfferDelayPeriod = 6.months

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

    (for {
      contentURL <- contentRefererOpt if Uri.parse(contentURL).host.contains("www.theguardian.com")
      access <- accessOpt
    } yield {
      Redirect(routes.MemberOnlyContent.membershipContent(contentURL, access.name))
    }).getOrElse(
      Ok(views.html.joiner.tierChooser(TouchpointBackend.Normal.catalog, pageInfo, eventOpt, accessOpt, signInUrl))
    ).withSession(
      request.session.copy(data = request.session.data ++ contentRefererOpt.map(JoinReferrer -> _))
    )
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

  def NonMemberAction(tier: Tier) = NoCacheAction andThen PlannedOutageProtection andThen authenticated() andThen onlyNonMemberFilter(onMember = redirectMemberAttemptingToSignUp(tier))

  def enterPaidDetails(
    tier: PaidTier,
    countryGroup: CountryGroup,
    promoCode: Option[PromoCode],
    pricingType: Option[BillingPeriod],
    paypalTest: Option[String]) = NonMemberAction(tier).async { implicit request =>

    implicit val resolution: TouchpointBackend.Resolution =
      TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)

    implicit val tpBackend = resolution.backend

    implicit val backendProvider: BackendProvider = new BackendProvider {
      override def touchpointBackend = tpBackend
    }
    implicit val c = catalog

    val identityRequest = IdentityRequest(request)

    (for {
      identityUser <- identityService.getIdentityUserView(request.user, identityRequest)
    } yield {
      val plans = catalog.findPaid(tier)
      val supportedCurrencies = plans.allPricing.map(_.currency).toSet
      val pageInfo = PageInfo(
        stripePublicKey = Some(stripeService.publicKey),
        payPalEnvironment = Some(tpBackend.payPalService.config.payPalEnvironment),
        initialCheckoutForm = CheckoutForm.forIdentityUser(identityUser, plans, Some(countryGroup), pricingType)
      )

      val providedPromoCode = promoCode orElse codeFromSession

      // is the providedPromoCode valid for the page being rendered (year is default billing period)
      val planChoice = PaidPlanChoice(tier, pricingType.getOrElse(BillingPeriod.year))
      val validPromoCode = providedPromoCode.flatMap(promoService.validate[NewUsers](_, pageInfo.initialCheckoutForm.defaultCountry.get, planChoice.productRatePlanId).toOption)
      val validPromotion = validPromoCode.flatMap(validPromo => promoService.findPromotion(validPromo.code))

      val validTrackingPromoCode = validPromotion.filter(_.asTracking.isDefined).flatMap(p => providedPromoCode)
      val validDisplayablePromoCode = validPromotion.filterNot(_.asTracking.isDefined).flatMap(p => providedPromoCode)

      val countryCurrencyWhitelist = CountryWithCurrency.whitelisted(supportedCurrencies, GBP)


      Ok(
      paypalTest match {
        case Some(variant) => views.html.joiner.form.paymentPayPal(
          plans,
          countryCurrencyWhitelist,
          identityUser,
          pageInfo,
          trackingPromoCode = validTrackingPromoCode,
          promoCodeToDisplay = validDisplayablePromoCode,
          Some(countryGroup),
          variant)
        case None => views.html.joiner.form.payment(
          plans,
          countryCurrencyWhitelist,
          identityUser,
          pageInfo,
          trackingPromoCode = validTrackingPromoCode,
          promoCodeToDisplay = validDisplayablePromoCode,
          Some(countryGroup),
          resolution)
      })
    }).andThen { case Failure(e) => logger.error(s"User ${request.user.user.id} could not enter details for paid tier ${tier.name}: ${identityRequest.trackingParameters}", e)}
  }

  def enterFriendDetails = NonMemberAction(Tier.friend).async { implicit request =>
    implicit val backendProvider: BackendProvider = request
    implicit val c = catalog

    for {
      identityUser <- identityService.getIdentityUserView(request.user, IdentityRequest(request))
    } yield {

      val pageInfo = support.PageInfo(initialCheckoutForm = CheckoutForm.forIdentityUser(identityUser, catalog.friend, None))
      val providedPromoCode = codeFromSession // only take from the session
      val validPromoCode = providedPromoCode.flatMap(promoService.validate[NewUsers](_, pageInfo.initialCheckoutForm.defaultCountry.get, catalog.friend.id).toOption)
      val validPromotion = validPromoCode.flatMap(validPromo => promoService.findPromotion(validPromo.code))
      val validTrackingPromoCode = validPromotion.filter(_.asTracking.isDefined).flatMap(p => providedPromoCode)

      Ok(views.html.joiner.form.friendSignup(
        catalog.friend,
        identityUser,
        pageInfo,
        validTrackingPromoCode))
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

  def joinPaid(tier: PaidTier) = AuthenticatedNonMemberAction.async { implicit request =>
    paidMemberJoinForm.bindFromRequest.fold({ formWithErrors =>
      Future.successful(BadRequest(formWithErrors.errorsAsJson))
    },
      makeMember(tier, Ok(Json.obj("redirect" -> routes.Joiner.thankyou(tier).url))))
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

  private def makeMember(tier: Tier, onSuccess: => Result)(formData: JoinForm)(implicit request: AuthRequest[_]) = {
    logger.info(s"User ${request.user.id} attempting to become ${tier.name}...")
    val eventId = PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(request.session)
    implicit val bp: BackendProvider = request
    val idRequest = IdentityRequest(request)
    val campaignCode = CampaignCode.fromRequest
    val ipAddress = None // Deprecated - we do not need to store this [anymore] as we store the ipCountry instead.
    val ipCountry = request.getFastlyCountry

    Timing.record(salesforceService.metrics, "createMember") {
      memberService.createMember(request.user, formData, idRequest, eventId, campaignCode, tier, ipAddress, ipCountry).map {
        case (sfContactId, zuoraSubName) =>
          logger.info(s"User ${request.user.id} successfully became ${tier.name} $zuoraSubName.")
          salesforceService.metrics.putSignUp(tier)
          trackRegistration(formData, tier, sfContactId, request.user, campaignCode)
          trackRegistrationViaEvent(sfContactId, request.user, eventId, campaignCode, tier)
          onSuccess
      }.recover {
        // errors due to user's card are logged at WARN level as they are not logic errors
        case error: Stripe.Error =>
          logger.warn(s"Stripe API call returned error: \n\t$error \n\tuser=${request.user.id}")
          Forbidden(Json.toJson(error))

        case error: PaymentGatewayError =>
          handlePaymentGatewayError(error, request.user.id, tier.name, idRequest.trackingParameters, formData.deliveryAddress.countryName)

        case error =>
          salesforceService.metrics.putFailSignUp(tier)
          logger.error(s"User ${request.user.id} could not become ${tier.name} member: ${idRequest.trackingParameters}", error)
          Forbidden
      }
    }
  }

  def thankyou(tier: Tier, upgrade: Boolean = false) = SubscriptionAction.async { implicit request =>
    implicit val resolution: TouchpointBackend.Resolution = TouchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
    val prpId = request.subscriber.subscription.plan.productRatePlanId
    implicit val idReq = IdentityRequest(request)

    for {
      country <- memberService.country(request.subscriber.contact)
      paymentSummary <- memberService.getMembershipSubscriptionSummary(request.subscriber.contact)
      promotion = request.subscriber.subscription.promoCode.flatMap(c => promoService.findPromotion(c))
      validPromotion = promotion.flatMap(_.validateFor(prpId, country).map(_ => promotion).toOption.flatten)
      destination <- request.touchpointBackend.destinationService.returnDestinationFor(request.session, request.subscriber)
      paymentMethod <- paymentService.getPaymentMethod(request.subscriber.subscription.accountId)
    } yield Ok(views.html.joiner.thankyou(
      request.subscriber,
      paymentSummary,
      paymentMethod,
      destination,
      upgrade,
      validPromotion.filterNot(_.asTracking.isDefined),
      resolution
    )).discardingCookies(TierChangeCookies.deletionCookies: _*)
  }

  def thankyouStaff = thankyou(Tier.partner)

  private def handlePaymentGatewayError(e: PaymentGatewayError, userId: String, tier: String, tracking: List[(String, String)], country: String) = {

    def handleError(code: String) = {
      logger.warn(s"User $userId could not become $tier member due to payment gateway failed transaction: \n\terror=$e \n\tuser=$userId \n\ttracking=$tracking \n\tcountry=$country")
      Forbidden(Json.obj("type" -> "PaymentGatewayError", "code" -> code))
    }

    e.errType match {
      case Fraudulent => handleError("Fraudulent")
      case TransactionNotAllowed => handleError("TransactionNotAllowed")
      case DoNotHonor => handleError("DoNotHonor")
      case InsufficientFunds => handleError("InsufficientFunds")
      case RevocationOfAuthorization => handleError("RevocationOfAuthorization")
      case GenericDecline => handleError("GenericDecline")
      case _ => handleError("UknownPaymentError")
    }
  }
}
