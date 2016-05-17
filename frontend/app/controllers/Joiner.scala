package controllers

import actions.ActionRefiners._
import actions.{RichAuthRequest, _}
import com.github.nscala_time.time.Imports._
import com.gu.contentapi.client.model.v1.{MembershipTier=>ContentAccess}
import com.gu.i18n.CountryGroup.UK
import com.gu.i18n.{CountryGroup, GBP}
import com.gu.memsub.BillingPeriod
import com.gu.memsub.promo.{NewUsers, PromoCode}
import com.gu.salesforce._
import com.gu.stripe.Stripe
import com.gu.stripe.Stripe.Serializer._
import com.gu.zuora.soap.models.errors._
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
import utils.{CampaignCode, TierChangeCookies}
import views.support
import views.support.Pricing._
import views.support.TierPlans._
import views.support.{CheckoutForm, CountryWithCurrency, PageInfo}

import scala.concurrent.Future
import scalaz.OptionT
import scalaz.std.scalaFuture._

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
    val eventOpt = PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(request).flatMap(EventbriteService.getBookableEvent)
    val accessOpt = request.getQueryString("membershipAccess").flatMap(ContentAccess.valueOf)
    val contentRefererOpt = request.headers.get(REFERER)

    val signInUrl = contentRefererOpt.map { referer =>
      ((Config.idWebAppUrl / "signin") ? ("returnUrl" -> referer) ? ("skipConfirmation" -> "true")).toString
    }.getOrElse(Config.idWebAppSigninUrl(""))

    implicit val countryGroup = UK
    val pageInfo = PageInfo(
      title=CopyConfig.copyTitleChooseTier,
      url=request.path,
      description=Some(CopyConfig.copyDescriptionChooseTier),
      customSignInUrl=Some(signInUrl)
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
        Ok(views.html.joiner.staff(catalog, new StaffEmails(request.user.email, Some(primaryEmailAddress)), displayName, avatarUrl, flashMsgOpt))

      case _ =>
        Future.successful(
          Ok(views.html.joiner.staff(catalog, new StaffEmails(request.user.email, None), None, None, flashMsgOpt)) )
    }
  }

  def NonMemberAction(tier: Tier) = AuthenticatedAction andThen onlyNonMemberFilter(onMember = redirectMemberAttemptingToSignUp(tier))

  def enterPaidDetails(tier: PaidTier, countryGroup: CountryGroup, promoCode: Option[PromoCode]) = NonMemberAction(tier).async { implicit request =>
    implicit val backendProvider: BackendProvider = request
    implicit val c = catalog

    for {
      identityUser <- identityService.getIdentityUserView(request.user, IdentityRequest(request))
    } yield {
      val plans = catalog.findPaid(tier)
      val supportedCurrencies = plans.allPricing.map(_.currency).toSet
      val pageInfo = PageInfo(
        stripePublicKey = Some(stripeService.publicKey),
        initialCheckoutForm = CheckoutForm.forIdentityUser(identityUser, plans, Some(countryGroup))
      )

      val providedPromoCode = promoCode orElse codeFromSession

      // is the providedPromoCode valid for the page being rendered (year is default billing period)
      val planChoice = PaidPlanChoice(tier, BillingPeriod.year)
      val validPromoCode = providedPromoCode.flatMap(promoService.validate[NewUsers](_, pageInfo.initialCheckoutForm.defaultCountry.get, planChoice.productRatePlanId).toOption)
      val validPromotion = validPromoCode.flatMap(validPromo => promoService.findPromotion(validPromo.code))

      val validTrackingPromoCode = validPromotion.filter(_.whenTracking.isDefined).flatMap(p => providedPromoCode)
      val validDisplayablePromoCode = validPromotion.filterNot(_.whenTracking.isDefined).flatMap(p => providedPromoCode)

      Ok(views.html.joiner.form.payment(
         plans = plans,
         countriesWithCurrencies = CountryWithCurrency.whitelisted(supportedCurrencies, GBP),
         idUser = identityUser,
         pageInfo = pageInfo,
         trackingPromoCode = validTrackingPromoCode,
         promoCodeToDisplay = validDisplayablePromoCode))
    }
  }

  def enterFriendDetails = NonMemberAction(Tier.friend).async { implicit request =>
    implicit val backendProvider: BackendProvider = request
    implicit val c = catalog

    for {
      identityUser <- identityService.getIdentityUserView(request.user, IdentityRequest(request))
    } yield {

      val pageInfo = support.PageInfo(initialCheckoutForm = CheckoutForm.forIdentityUser(identityUser, catalog.friend, None))
      val providedPromoCode = codeFromSession // only take from the session
      val validPromoCode = providedPromoCode.flatMap(promoService.validate[NewUsers](_, pageInfo.initialCheckoutForm.defaultCountry.get, catalog.friend.productRatePlanId).toOption)
      val validPromotion = validPromoCode.flatMap(validPromo => promoService.findPromotion(validPromo.code))
      val validTrackingPromoCode = validPromotion.filter(_.whenTracking.isDefined).flatMap(p => providedPromoCode)

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
      makeMember(Tier.friend, Redirect(routes.Joiner.thankyou(Tier.friend))) )
  }

  def joinStaff = AuthenticatedNonMemberAction.async { implicit request =>
    staffJoinForm.bindFromRequest.fold(redirectToUnsupportedBrowserInfo,
        makeMember(Tier.partner, Redirect(routes.Joiner.thankyouStaff())) )
  }

  def joinPaid(tier: PaidTier) = AuthenticatedNonMemberAction.async { implicit request =>
    paidMemberJoinForm.bindFromRequest.fold({ formWithErrors =>
        Future.successful(BadRequest(formWithErrors.errorsAsJson))
      },
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

  private def makeMember(tier: Tier, onSuccess: => Result)(formData: JoinForm)(implicit request: AuthRequest[_]) = {
    val eventId = PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(request)
    implicit val bp: BackendProvider = request
    val idRequest = IdentityRequest(request)
    memberService.createMember(request.user, formData, idRequest, eventId, CampaignCode.fromRequest)
      .map{_ => onSuccess} recover {
        case error: Stripe.Error =>
          logger.warn(s"Stripe API call returned error: \n\t${error} \n\tuser=${request.user.id}")
          Forbidden(Json.toJson(error))

        case error: PaymentGatewayError => handlePaymentGatewayError(error, request.user.id)

        case error =>
          logger.error(s"User ${request.user.id} could not become a member: ${idRequest.trackingParameters}", error)
          Forbidden
    }
  }

  def thankyou(tier: Tier, upgrade: Boolean = false) = SubscriptionAction.async { implicit request =>
    val prpId = request.subscriber.subscription.productRatePlanId
    implicit val idReq = IdentityRequest(request)

    val paymentCard = (for {
      card <- OptionT(paymentService.getPaymentCard(request.subscriber.subscription.accountId))
    } yield card).run

    for {
      country <- memberService.country(request.subscriber.contact)
      paymentSummary <- memberService.getMembershipSubscriptionSummary(request.subscriber.contact)
      promotion = request.subscriber.subscription.promoCode.flatMap(c => promoService.findPromotion(c))
      validPromotion = promotion.flatMap(_.validateFor(prpId, country).map(_ => promotion).toOption.flatten)
      destination <- DestinationService.returnDestinationFor(request)
      card <- paymentCard
    } yield Ok(views.html.joiner.thankyou(
        request.subscriber,
        paymentSummary,
        card,
        destination,
        upgrade,
        validPromotion.filterNot(_.whenTracking.isDefined)
    )).discardingCookies(TierChangeCookies.deletionCookies:_*)
  }

  def thankyouStaff = thankyou(Tier.partner)

  private def handlePaymentGatewayError(e: PaymentGatewayError, userId: String) = {

    def handleError(msg: String, errType: String) = {
      logger.warn(s"${msg}: \n\t${e} \n\tuser=${userId}")
      Forbidden(Json.obj("type" -> errType, "message" -> msg))
    }

    // TODO: Does Zuora provide a guarantee the message is safe to display to users directly?
    // For now providing custom message to make sure no sensitive information is revealed.

    logger.warn(s"User ${userId} could not become a member due to payment gateway failed transaction.")

    e.errType match {
      case InsufficientFunds =>
        handleError("Your card has insufficient funds", "InssufficientFunds")

      case TransactionNotAllowed =>
        handleError("Your card does not support this type of purchase", "TransactionNotAllowed")

      case RevocationOfAuthorization =>
        handleError(
          "Cardholder has requested all payments to be stopped on this card",
          "RevocationOfAuthorization")

      case _ =>
        handleError("Your card was declined", "PaymentGatewayError")
    }
  }
}
