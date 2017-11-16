package actions

import abtests.{ABTest, AudienceId}
import actions.ActionRefiners._
import actions.Fallbacks._
import com.gu.googleauth
import com.gu.salesforce.PaidTier
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import controllers._
import play.api.http.HeaderNames._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.Results._
import play.api.mvc._
import services.{AuthenticationService, TouchpointBackend}
import utils.GuMemCookie
import utils.TestUsers.{PreSigninTestCookie, isTestUser}

import scala.concurrent.Future

trait CommonActions extends LazyLogging {

  val AddAbTestingCookiesToResponse = new ActionBuilder[Request] {
    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) =
      block(request).map { result =>
        val newABTestCookies = ABTest.cookiesWhichShouldBeDropped(request)
        if (newABTestCookies.nonEmpty) {
          val testUser = isTestUser(PreSigninTestCookie, request.cookies)(request).isDefined
          logger.info(s"dropping-new-ab-test-cookies (path=${request.path} testUser=$testUser) : ${newABTestCookies.map(c => s"${c.name}=${c.value}").mkString(" ")}")
        }
        result.withCookies(newABTestCookies ++ AudienceId.cookieWhichShouldBeDropped(request) :_*)
      }
  }

  val AddUserInfoToResponse = new ActionBuilder[Request] {
    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) =
      block(request).map { result =>
        (for (user <- AuthenticationService.authenticatedUserFor(request)) yield {
          result.withHeaders(
            "X-Gu-Identity-Id" -> user.id,
            "X-Gu-Membership-Test-User" -> isTestUser(user).toString)
        }).getOrElse(result)
      }
    }

  val NoCacheAction = resultModifier(NoCache(_)) andThen AddUserInfoToResponse andThen AddAbTestingCookiesToResponse

  val CachedAction = resultModifier(Cached(_))

  val Cors = new ActionBuilder[Request] {
    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) = {
      block(request).map { result =>
        (for (originHeader <- request.headers.get(ORIGIN) if Config.corsAllowOrigin.contains(originHeader)) yield {
          result.withHeaders(
            ACCESS_CONTROL_ALLOW_ORIGIN -> originHeader,
            ACCESS_CONTROL_ALLOW_CREDENTIALS -> "true")
        }).getOrElse(result)
      }
    }
  }

  val CorsPublic = resultModifier { result =>
    result.withHeaders(
      ACCESS_CONTROL_ALLOW_ORIGIN -> "*",
      ACCESS_CONTROL_ALLOW_CREDENTIALS -> "true"
    )
  }

  val AuthenticatedAction = NoCacheAction andThen authenticated()

  val OptionallyAuthenticatedAction = NoCacheAction andThen new ActionTransformer[Request, OptionallyAuthenticatedRequest]{
    override protected def transform[A](request: Request[A]): Future[OptionallyAuthenticatedRequest[A]] = {
      val user = AuthenticationService.authenticatedUserFor(request)
      val touchpointBackend = user.fold(TouchpointBackend.Normal)(TouchpointBackend.forUser(_))
      Future.successful(OptionallyAuthenticatedRequest[A](touchpointBackend,user,request))
    }
  }

  val AuthenticatedNonMemberAction = AuthenticatedAction andThen onlyNonMemberFilter()

  val SubscriptionAction = AuthenticatedAction andThen subscriptionRefiner()

  val ContributorAction = AuthenticatedAction andThen contributionRefiner()

  val StaffMemberAction = AuthenticatedAction andThen subscriptionRefiner(onNonMember = joinStaffMembership(_))

  val PaidSubscriptionAction = SubscriptionAction andThen paidSubscriptionRefiner()

  val FreeSubscriptionAction = SubscriptionAction andThen freeSubscriptionRefiner()

  val CorsPublicCachedAction = CorsPublic andThen CachedAction

  val AjaxAuthenticatedAction = Cors andThen NoCacheAction andThen authenticated(onUnauthenticated = setGuMemCookie(_))

  val AjaxSubscriptionAction = AjaxAuthenticatedAction andThen subscriptionRefiner(onNonMember = setGuMemCookie(_))

  val AjaxPaidSubscriptionAction = AjaxSubscriptionAction andThen paidSubscriptionRefiner(onFreeMember = _ => Forbidden)

  val StoreAcquisitionDataAction = new ActionBuilder[Request] {
    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] =
      block(request).map { result =>
        request.getQueryString("acquisitionData").fold(result)(acquisitionData =>
          result.withSession(request.session + ("acquisitionData" -> acquisitionData))
        )
      }
  }

  def setGuMemCookie(implicit request: RequestHeader) =
    AuthenticationService.authenticatedUserFor(request).fold(Forbidden.discardingCookies(GuMemCookie.deletionCookie)) { user =>
      val json = Json.obj("userId" -> user.id)
      Ok(json).withCookies(GuMemCookie.getAdditionCookie(json))
    }

  def ChangeToPaidAction(targetTier: PaidTier) = SubscriptionAction andThen checkTierChangeTo(targetTier)
}

trait OAuthActions extends googleauth.Actions with googleauth.Filters {
  val authConfig = Config.googleAuthConfig

  //routes
  override val loginTarget = routes.OAuth.loginAction()
  override val defaultRedirectTarget = routes.FrontPage.welcome()
  override val failureRedirectTarget = routes.OAuth.login()

  lazy val groupChecker = Config.googleGroupChecker

  val GoogleAuthAction: ActionBuilder[GoogleAuthRequest] = AuthAction
  val GoogleAuthenticatedStaffAction = NoCacheAction andThen GoogleAuthAction
  val permanentStaffGroups = Config.staffAuthorisedEmailGroups

  val PermanentStaffNonMemberAction =
    GoogleAuthenticatedStaffAction andThen requireGroup[GoogleAuthRequest](permanentStaffGroups, unauthorisedStaff(views.html.fragments.oauth.staffUnauthorisedError())(_))

  val AuthorisedStaff =
    GoogleAuthenticatedStaffAction andThen
      requireGroup[GoogleAuthRequest](permanentStaffGroups, unauthorisedStaff(views.html.fragments.oauth.staffWrongGroup())(_))

  def AuthenticatedStaffNonMemberAction =
    AuthenticatedAction andThen
      onlyNonMemberFilter() andThen
      googleAuthenticationRefiner andThen
      requireGroup[IdentityGoogleAuthRequest](permanentStaffGroups, unauthorisedStaff(views.html.fragments.oauth.staffUnauthorisedError())(_))

  def googleAuthenticationRefiner = {
    new ActionRefiner[AuthRequest, IdentityGoogleAuthRequest] {
      override def refine[A](request: AuthRequest[A]) = Future.successful {
        userIdentity(request).map(IdentityGoogleAuthRequest(_, request)).toRight(sendForAuth(request))
      }
    }
  }
}
