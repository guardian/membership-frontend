package controllers

import actions.Functions._
import actions.{RichAuthRequest, _}
import com.github.nscala_time.time.Imports._
import com.gu.i18n.{CountryGroup, GBP}
import com.gu.membership.model.Year
import com.gu.membership.salesforce.Tier.Friend
import com.gu.membership.salesforce._
import com.gu.membership.stripe.Stripe
import com.gu.membership.stripe.Stripe.Serializer._
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
import services.{GuardianContentService, _}
import tracking.ActivityTracking
import utils.TierChangeCookies
import views.support
import views.support.PageInfo.CheckoutForm
import views.support.{CountryWithCurrency, PageInfo}

import scala.concurrent.Future

object Joiner extends Controller with ActivityTracking
                                 with LazyLogging
                                 with CatalogProvider
                                 with StripeServiceProvider
                                 with SalesforceServiceProvider
                                 with MemberServiceProvider {
  val JoinReferrer = "join-referrer"

  val contentApiService = GuardianContentService


  val subscriberOfferDelayPeriod = 6.months

  val casService = CASService

  val EmailMatchingGuardianAuthenticatedStaffNonMemberAction = AuthenticatedStaffNonMemberAction andThen matchingGuardianEmail()

  val identityService = IdentityService(IdentityApi)

  def tierChooser = NoCacheAction.async { implicit request =>
    val eventOpt = PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(request).flatMap(EventbriteService.getBookableEvent)
    val accessOpt = request.getQueryString("membershipAccess").map(MembershipAccess)
    val contentRefererOpt = request.headers.get(REFERER)

    val signInUrl = contentRefererOpt.map { referer =>
      ((Config.idWebAppUrl / "signin") ? ("returnUrl" -> referer) ? ("skipConfirmation" -> "true")).toString
    }.getOrElse(Config.idWebAppSigninUrl(""))

    implicit val currency = GBP
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
    implicit val currency = GBP

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

  def NonMemberAction(tier: Tier) = AuthenticatedAction andThen onlyNonMemberFilter(onMember = redirectMemberAttemptingToSignUp(tier))

  def enterPaidDetails(tier: PaidTier, countryGroup: CountryGroup) = NonMemberAction(tier).async { implicit request =>
    implicit val backendProvider: BackendProvider = request
    val desiredCurrency = countryGroup.currency
    for {
      cat <- catalog
      identityUser <- identityService.getIdentityUserView(request.user, IdentityRequest(request))
    } yield {
      val paidDetails = cat.paidTierDetails(tier)
      val supportedCurrencies = paidDetails.currencies
      val currency = if (supportedCurrencies.contains(desiredCurrency)) desiredCurrency else GBP
      val idUserWithCountry = identityUser.withCountryGroup(countryGroup)
      val pageInfo = PageInfo(
        stripePublicKey = Some(stripeService.publicKey),
        initialCheckoutForm = CheckoutForm(countryGroup.defaultCountry, currency, Year)
      )

      Ok(views.html.joiner.form.payment(
         countriesWithCurrencies = CountryWithCurrency.whitelisted(supportedCurrencies, GBP),
         details = paidDetails,
         idUser = idUserWithCountry,
         pageInfo = pageInfo))
    }
  }

  def enterFriendDetails = NonMemberAction(Friend).async { implicit request: AuthRequest[_] =>
    implicit val backendProvider: BackendProvider = request
    for {
      identityUser <- identityService.getIdentityUserView(request.user, IdentityRequest(request))
      cat <- catalog
    } yield {
      val ukGroup = CountryGroup.UK
      val formI18n = CheckoutForm(ukGroup.defaultCountry, ukGroup.currency, Year)
      Ok(views.html.joiner.form.friendSignup(
        cat.friend,
        identityUser,
        support.PageInfo(initialCheckoutForm = formI18n)))
    }
  }

  def enterStaffDetails = EmailMatchingGuardianAuthenticatedStaffNonMemberAction.async { implicit request =>
    val flashMsgOpt = request.flash.get("success").map(FlashMessage.success)
    implicit val backendProvider: BackendProvider = request
    for {
      identityUser <- identityService.getIdentityUserView(request.identityUser, IdentityRequest(request))
      cat <- catalog
    } yield {
      Ok(views.html.joiner.form.addressWithWelcomePack(cat.staff, identityUser, flashMsgOpt))
    }
  }

  def joinFriend = AuthenticatedNonMemberAction.async { implicit request =>
    friendJoinForm.bindFromRequest.fold(redirectToUnsupportedBrowserInfo,
      makeMember(Tier.Friend, Redirect(routes.Joiner.thankyou(Tier.Friend))) )
  }

  def joinStaff = AuthenticatedNonMemberAction.async { implicit request =>
    staffJoinForm.bindFromRequest.fold(redirectToUnsupportedBrowserInfo,
        makeMember(Tier.Partner, Redirect(routes.Joiner.thankyouStaff())) )
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
    memberService.createMember(request.user, formData, IdentityRequest(request), eventId)
      .map(_ => onSuccess) recover {
      case error: Stripe.Error => Forbidden(Json.toJson(error))
      case error =>
        logger.error("An error occurred while calling Joiner.makeMember", error)
        Forbidden
    }
  }

  def thankyou(tier: Tier, upgrade: Boolean = false) = MemberAction.async { implicit request =>
    for {
      paymentSummary <- memberService.getMembershipSubscriptionSummary(request.member)
      stripeCustomer <- salesforceService.getStripeCustomer(request.member)
      destination <- DestinationService.returnDestinationFor(request)
    } yield Ok(views.html.joiner.thankyou(
        request.member,
        paymentSummary,
        stripeCustomer.map(_.card),
        destination,
        upgrade
    )).discardingCookies(TierChangeCookies.deletionCookies:_*)
  }

  def thankyouStaff = thankyou(Tier.Partner)
}
