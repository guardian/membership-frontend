package controllers


import abtests.ABTest
import actions.ActionRefiners._
import actions.Fallbacks.chooseRegister
import actions.{RichAuthRequest, _}
import com.github.nscala_time.time.Imports._
import com.gu.contentapi.client.model.v1.{MembershipTier => ContentAccess}
import com.gu.googleauth.GoogleAuthConfig
import com.gu.i18n.CountryGroup
import com.gu.i18n.CountryGroup.UK
import com.gu.i18n.Currency.GBP
import com.gu.salesforce._
import com.gu.stripe.Stripe
import com.gu.stripe.Stripe.Serializer._
import com.gu.zuora.soap.models.errors._
import io.lemonlabs.uri.dsl._
import com.gu.monitoring.SafeLogger
import com.gu.monitoring.SafeLogger._
import configuration.{Config, CopyConfig}
import forms.MemberForm.{paidMemberJoinForm, _}
import model._
import play.api.i18n.Messages.Implicits._
import play.api.i18n.{I18nSupport, Messages, MessagesApi}
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc._
import services.api.MemberService.CreateMemberResult
import services.checkout.identitystrategy.StrategyDecider
import services.{GuardianContentService, _}
import tracking.AcquisitionTracking
import utils.RequestCountry._
import utils.TestUsers.{NameEnteredInForm, PreSigninTestCookie, isTestUser}
import utils.{Feature, ReferralData, TestUsers, TierChangeCookies}
import views.support
import views.support.MembershipCompat._
import views.support.Pricing._
import views.support.TierPlans._
import views.support.{CheckoutForm, CountryWithCurrency, IdentityUser, PageInfo}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Failure

class Joiner(
  override val wsClient: WSClient,
  val identityApi: IdentityApi,
  implicit val eventbriteService: EventbriteCollectiveServices,
  contentApiService: GuardianContentService,
  implicit val touchpointBackend: TouchpointBackends,
  touchpointOAuthActions: TouchpointOAuthActions,
  touchpointActionRefiners: TouchpointActionRefiners,
  touchpointCommonActions: TouchpointCommonActions,
  implicit val parser: BodyParser[AnyContent],
  override implicit val executionContext: ExecutionContext,
  googleAuthConfig: GoogleAuthConfig,
  commonActions: CommonActions,
  actionRefiners: ActionRefiners,
  membersDataAPI: MembersDataAPI,
  authenticationService: AuthenticationService,
  testUsers: TestUsers,
  strategyDecider: StrategyDecider,
  override protected val controllerComponents: ControllerComponents
) extends OAuthActions(parser, executionContext, googleAuthConfig, commonActions)
  with BaseController
  with I18nSupport
  with AcquisitionTracking
  with PaymentGatewayErrorHandler
  with CatalogProvider
  with StripeUKMembershipServiceProvider
  with StripeAUMembershipServiceProvider
  with SalesforceServiceProvider
  with SubscriptionServiceProvider
  with PaymentServiceProvider
  with MemberServiceProvider
  with ZuoraRestServiceProvider {

  import actionRefiners.{PlannedOutageProtection, redirectMemberAttemptingToSignUp, authenticated}
  import touchpointOAuthActions._
  import touchpointActionRefiners._
  import touchpointCommonActions._
  import commonActions.{CachedAction, NoCacheAction}

  val JoinReferrer = "join-referrer"

  val subscriberOfferDelayPeriod = 6.months

  val identityService = IdentityService(identityApi)

  def tierChooser = NoCacheAction { implicit request =>
    val eventOpt = PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(request.session).flatMap(eventbriteService.getBookableEvent)
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

    Ok(views.html.joiner.tierChooser(touchpointBackend.Normal.catalog, pageInfo, eventOpt, accessOpt, signInUrl))
      .withSession(request.session.copy(data = request.session.data ++ contentRefererOpt.map(JoinReferrer -> _)))
  }

  def authenticatedIfNotMergingRegistration(onUnauthenticated: RequestHeader => Result = chooseRegister(_)) = new ActionFilter[Request] {

    override protected def executionContext = Joiner.this.executionContext

    override def filter[A](req: Request[A]) = Future.successful {
      val userOpt = authenticationService.authenticatedUserFor(req)
      val userSignedIn = userOpt.isDefined
      val canWaiveAuth = Feature.MergedRegistration.turnedOnFor(req)
      val canAccess = userSignedIn || canWaiveAuth
      SafeLogger.info(s"optional-auth ${req.path} canWaiveAuth=$canWaiveAuth userSignedIn=$userSignedIn canAccess=$canAccess testUser=${testUsers.isTestUser(PreSigninTestCookie, req.cookies)(req).isDefined}")
      if (canAccess) None else Some(onUnauthenticated(req))
    }
  }

  def OptionallyAuthenticatedNonMemberAction(tier: Tier) =
    NoCacheAction andThen PlannedOutageProtection andThen authenticatedIfNotMergingRegistration() andThen noAuthenticatedMemberFilter(onMember = redirectMemberAttemptingToSignUp(tier))

  def NonMemberAction(tier: Tier) =
    NoCacheAction andThen PlannedOutageProtection andThen authenticated() andThen onlyNonMemberFilter(onMember = redirectMemberAttemptingToSignUp(tier))

  def enterPaidDetails(
    tier: PaidTier,
    countryGroup: CountryGroup) = OptionallyAuthenticatedNonMemberAction(tier).async { implicit request =>
    val userOpt = authenticationService.authenticatedUserFor(request)
    implicit val resolution: TouchpointBackend.Resolution =
      touchpointBackend.forRequest(PreSigninTestCookie, request.cookies)

    implicit val tpBackend = resolution.backend

    implicit val backendProvider: BackendProvider = new BackendProvider {
      override def touchpointBackend(implicit tpbs: TouchpointBackends) = tpBackend
    }
    implicit val c = catalog

    val identityRequest = IdentityRequest(request)

    (for {
      identityUserOpt <- userOpt.map(user => identityService.getIdentityUserView(user.minimalUser, identityRequest).map(Option(_))).getOrElse(Future.successful[Option[IdentityUser]](None))
    } yield {

      for (identityUser <- identityUserOpt) {
        SafeLogger.info(s"signed-in-enter-details tier=${tier.slug} testUser=${identityUser.isTestUser} passwordExists=${identityUser.passwordExists} ${ABTest.allTests.map(_.describeParticipation).mkString(" ")}")
      }
      val plans = catalog.findPaid(tier)
      val supportedCurrencies = plans.allPricing.map(_.currency).toSet
      val pageInfo = PageInfo(
        stripeUKMembershipPublicKey = Some(stripeUKMembershipService.publicKey),
        stripeAUMembershipPublicKey = Some(stripeAUMembershipService.publicKey),
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
        countryGroup,
        resolution))
    }).andThen { case Failure(e) => SafeLogger.error(scrub"User ${userOpt.map(_.minimalUser.id)} could not enter details for paid tier ${tier.name}: ${identityRequest.trackingParameters}", e)}
  }


  def joinStaff = AuthenticatedNonMemberAction.async { implicit request =>
    staffJoinForm.bindFromRequest.fold(redirectToUnsupportedBrowserInfo,
      makeMember(Tier.partner, Redirect(routes.Joiner.thankyouStaff())))
  }

  def joinPaid(tier: PaidTier) = OptionallyAuthenticatedNonMemberAction(tier).async { implicit request =>
    paidMemberJoinForm.bindFromRequest.fold({ formWithErrors =>
      SafeLogger.info(s"There was an error in the form submitted to join-paid: ${formWithErrors.errorsAsJson}")
      Future.successful(BadRequest(formWithErrors.errorsAsJson))
    },
      makeMember(tier, Ok(Json.obj("redirect" -> routes.Joiner.thankyou(tier).url))))
  }

  def unsupportedBrowser = CachedAction(Ok(views.html.joiner.unsupportedBrowser()))

  private def makeMember(tier: Tier, onSuccess: => Result)(formData: JoinForm)(implicit request: Request[_]) = {
    val userOpt = authenticationService.authenticatedUserFor(request)
    SafeLogger.info(s"${s"User id=${userOpt.map(_.minimalUser.id).mkString}"} attempting to become ${tier.name}...")
    val eventId = PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(request.session)
    implicit val resolution: TouchpointBackend.Resolution =
      touchpointBackend.forRequest(NameEnteredInForm, Some(formData))

    implicit val tpBackend = resolution.backend
    implicit val backendProvider: BackendProvider = new BackendProvider {
      override def touchpointBackend(implicit tpbs: TouchpointBackends) = tpBackend
    }
    val referralData = ReferralData.fromRequest
    val ipCountry = request.getFastlyCountry

    val identityStrategy = strategyDecider.identityStrategyFor(identityService, request, formData)
    identityStrategy.ensureIdUser { user =>
      salesforceService.metrics.putAttemptedSignUp(tier)
      memberService.createMember(user, formData, eventId, tier, ipCountry, referralData).map {
        case CreateMemberResult(sfContactId, zuoraSubName) =>
          val minimalUser = IdMinimalUser(user.id, user.publicFields.displayName)
          SafeLogger.info(s"make-member-success ${tier.name} ${ABTest.allTests.map(_.describeParticipationFromCookie).mkString(" ")} ${identityStrategy.getClass.getSimpleName} user=${user.id} testUser=${isTestUser(minimalUser)} suppliedNewPassword=${formData.password.isDefined} sub=$zuoraSubName")
          if (formData.marketingConsent)
            identityService.consentEmail(user.primaryEmailAddress, IdentityRequest(request))

          salesforceService.metrics.putSignUp(tier)
          formData match {
            case paid: PaidMemberJoinForm =>
              onSuccess.withSession(paid.pageviewId.map(id => request.session + ("pageviewId" -> id)).getOrElse(request.session))
            case _ =>
              onSuccess
          }
      }.recover {
        // errors due to user's card are logged at WARN level as they are not logic errors
        case error: Stripe.Error =>
          salesforceService.metrics.putFailSignUpStripe(tier)
          SafeLogger.warn(s"Stripe API call returned error: \n\t$error \n\tuser=$userOpt")
          Forbidden(Json.toJson(error))

        case error: PaymentGatewayError =>
          salesforceService.metrics.putFailSignUpGatewayError(tier)
          handlePaymentGatewayError(error, user.id, tier.name, formData.deliveryAddress.countryName)

        case error =>
          salesforceService.metrics.putFailSignUp(tier)
          SafeLogger.error(scrub"${s"User id=${userOpt.map(_.minimalUser.id).mkString}"} could not become ${tier.name} member", error)
          Forbidden
      }
    }
  }

  def thankyou(tier: Tier, upgrade: Boolean = false) = SubscriptionAction.async { implicit request =>
    implicit val resolution: TouchpointBackend.Resolution = touchpointBackend.forRequest(PreSigninTestCookie, request.cookies)
    implicit val idReq = IdentityRequest(request)

    val emailFromZuora = zuoraRestService.getAccount(request.subscriber.subscription.accountId) map { account =>
      account.toOption.flatMap(_.billToContact.email)
    }

    import scalaz.std.scalaFuture._

    val destinationService = new DestinationService[Future](
      eventbriteService.getBookableEvent,
      contentApiService.contentItemQuery,
      memberService.createEBCode
    )

    for {
      paymentSummary <- memberService.getMembershipSubscriptionSummary(request.subscriber.contact)
      destination <- destinationService.returnDestinationFor(request.session, request.subscriber)
      paymentMethod <- paymentService.getPaymentMethod(request.subscriber.subscription.accountId)
      email <- emailFromZuora
    } yield {
      tier match {
        case t: Tier if !upgrade => salesforceService.metrics.putThankYou(tier)
        case _ =>
      }
      SafeLogger.info(s"thank you displayed for user: ${request.user.minimalUser.id} subscription: ${request.subscriber.subscription.accountId.get} tier: ${tier.name}")

      trackAcquisition(paymentSummary, paymentMethod, tier, request)

      Ok({views.html.joiner.thankyou(
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

}
