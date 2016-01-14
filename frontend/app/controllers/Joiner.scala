package controllers

import actions.Functions._
import actions.{RichAuthRequest, _}
import com.github.nscala_time.time.Imports._
import com.gu.i18n.{CountryGroup, GBP}
import CountryGroup.UK

import com.gu.memsub.BillingPeriod.year
import com.gu.memsub.ProductFamily
import com.gu.salesforce._
import com.gu.stripe.Stripe
import com.gu.stripe.Stripe.Serializer._
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
import views.support.Pricing._
import views.support.{CountryWithCurrency, PageInfo}
import scalaz.std.scalaFuture._
import scalaz.syntax.monad._
import scalaz.syntax.std.option._
import scalaz.{MonadTrans, OptionT}
import scala.concurrent.Future

object Joiner extends Controller with ActivityTracking
                                 with LazyLogging
                                 with CatalogProvider
                                 with StripeServiceProvider
                                 with SalesforceServiceProvider
                                 with SubscriptionServiceProvider
                                 with PaymentServiceProvider
                                 with MemberServiceProvider {
  val JoinReferrer = "join-referrer"

  val contentApiService = GuardianContentService

  val subscriberOfferDelayPeriod = 6.months

  val EmailMatchingGuardianAuthenticatedStaffNonMemberAction = AuthenticatedStaffNonMemberAction andThen matchingGuardianEmail()

  val identityService = IdentityService(IdentityApi)

  def tierChooser = NoCacheAction { implicit request =>
    val eventOpt = PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(request).flatMap(EventbriteService.getBookableEvent)
    val accessOpt = request.getQueryString("membershipAccess").map(MembershipAccess)
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
        fullUser <- IdentityService(IdentityApi).getFullUserDetails(user, IdentityRequest(request))
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

  def enterPaidDetails(tier: PaidTier, countryGroup: CountryGroup) = NonMemberAction(tier).async { implicit request =>
    implicit val backendProvider: BackendProvider = request
    val desiredCurrency = countryGroup.currency
    for {
      identityUser <- identityService.getIdentityUserView(request.user, IdentityRequest(request))
    } yield {
      val plans = catalog.findPaid(tier)
      val supportedCurrencies = plans.allPricing.map(_.currency).toSet
      val currency = if (supportedCurrencies.contains(desiredCurrency)) desiredCurrency else GBP
      val idUserWithCountry = identityUser.withCountryGroup(countryGroup)
      val pageInfo = PageInfo(
        stripePublicKey = Some(stripeService.publicKey),
        initialCheckoutForm = CheckoutForm(countryGroup.defaultCountry, currency, year)
      )

      Ok(views.html.joiner.form.payment(
         plans = plans,
         countriesWithCurrencies = CountryWithCurrency.whitelisted(supportedCurrencies, GBP),
         idUser = idUserWithCountry,
         pageInfo = pageInfo))
    }
  }

  def enterFriendDetails = NonMemberAction(Tier.friend).async { implicit request: AuthRequest[_] =>
    implicit val backendProvider: BackendProvider = request
    for {
      identityUser <- identityService.getIdentityUserView(request.user, IdentityRequest(request))
    } yield {
      val ukGroup = CountryGroup.UK
      val formI18n = CheckoutForm(ukGroup.defaultCountry, ukGroup.currency, year)
      Ok(views.html.joiner.form.friendSignup(
        catalog.friend,
        identityUser,
        support.PageInfo(initialCheckoutForm = formI18n)))
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
    memberService.createMember(request.user, formData, IdentityRequest(request), eventId)
      .map(_ => onSuccess) recover {
      case error: Stripe.Error => Forbidden(Json.toJson(error))
      case error =>
        logger.error("An error occurred while calling Joiner.makeMember", error)
        Forbidden
    }
  }

  def thankyou(tier: Tier, upgrade: Boolean = false) = MemberAction.async { implicit request =>

    val paymentCard = (for {
      sub <- OptionT(subscriptionService.get(request.member)(ProductFamily.membership))
      card <- OptionT(paymentService.getPaymentCardByAccount(sub.accountId))
    } yield card).run

    for {
      paymentSummary <- memberService.getMembershipSubscriptionSummary(request.member)
      destination <- DestinationService.returnDestinationFor(request)
      card <- paymentCard
    } yield Ok(views.html.joiner.thankyou(
        request.member,
        paymentSummary,
        card,
        destination,
        upgrade
    )).discardingCookies(TierChangeCookies.deletionCookies:_*)
  }

  def thankyouStaff = thankyou(Tier.partner)
}
