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

}
