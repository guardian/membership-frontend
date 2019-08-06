package actions

import abtests.{ABTest, AudienceId}
import actions.Fallbacks._
import com.gu.googleauth
import com.gu.googleauth.GoogleAuthConfig
import com.gu.salesforce.PaidTier
import com.gu.monitoring.SafeLogger
import configuration.Config
import controllers._
import play.api.http.HeaderNames._
import play.api.libs.json.Json
import play.api.libs.ws.WSClient
import play.api.mvc.Results._
import play.api.mvc._
import services.{AuthenticationService, TouchpointBackend}
import utils.{GuMemCookie, TestUsers}
import utils.TestUsers.{PreSigninTestCookie, isTestUser}

import scala.concurrent.{ExecutionContext, Future}

class CommonActions(authenticationService: AuthenticationService, testUsers: TestUsers, parser: BodyParser[AnyContent], implicit val executionContext: ExecutionContext, actionRefiners: ActionRefiners) {

  import actionRefiners.{authenticated, resultModifier}

  val AddAbTestingCookiesToResponse = new ActionBuilder[Request, AnyContent] {

    override def parser = CommonActions.this.parser

    override protected implicit def executionContext: ExecutionContext = CommonActions.this.executionContext

    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) =
      block(request).map { result =>
        val newABTestCookies = ABTest.cookiesWhichShouldBeDropped(request)
        if (newABTestCookies.nonEmpty) {
          val testUser = testUsers.isTestUser(PreSigninTestCookie, request.cookies)(request).isDefined
          SafeLogger.info(s"dropping-new-ab-test-cookies (path=${request.path} testUser=$testUser) : ${newABTestCookies.map(c => s"${c.name}=${c.value}").mkString(" ")}")
        }
        result.withCookies(newABTestCookies ++ AudienceId.cookieWhichShouldBeDropped(request) :_*)
      }
  }

  val AddUserInfoToResponse = new ActionBuilder[Request, AnyContent] {

    override def parser = CommonActions.this.parser

    override protected implicit def executionContext: ExecutionContext = CommonActions.this.executionContext

    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]) =
      block(request).map { result =>
        (for (user <- authenticationService.authenticateUser(request)) yield {
          result.withHeaders(
            "X-Gu-Identity-Id" -> user.minimalUser.id,
            "X-Gu-Membership-Test-User" -> isTestUser(user.minimalUser).toString)
        }).getOrElse(result)
      }
    }

  val NoCacheAction = resultModifier(NoCache(_)) andThen AddUserInfoToResponse andThen AddAbTestingCookiesToResponse

  val CachedAction = resultModifier(Cached(_))

  val Cors = new ActionBuilder[Request, AnyContent] {

    override def parser = CommonActions.this.parser

    override protected implicit def executionContext: ExecutionContext = CommonActions.this.executionContext

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

  val AuthenticatedAction = NoCacheAction andThen authenticated()(parser)

  val CorsPublicCachedAction = CorsPublic andThen CachedAction

  val AjaxAuthenticatedAction = Cors andThen NoCacheAction andThen authenticated(onUnauthenticated = setGuMemCookie(_))(parser)

  val StoreAcquisitionDataAction = new ActionBuilder[Request, AnyContent] {
    import CommonActions._

    override def parser = CommonActions.this.parser

    override protected implicit def executionContext: ExecutionContext = CommonActions.this.executionContext

    def invokeBlock[A](request: Request[A], block: (Request[A]) => Future[Result]): Future[Result] =
      block(request).map { result =>
        request.getQueryString(acquisitionDataSessionKey).fold(result)(acquisitionData =>
          result.withSession(request.session + (acquisitionDataSessionKey -> acquisitionData))
        )
      }
  }

  def setGuMemCookie(implicit request: RequestHeader) =
    authenticationService.authenticateUser(request).fold(Forbidden.discardingCookies(GuMemCookie.deletionCookie)) { user =>
      val json = Json.obj("userId" -> user.minimalUser.id)
      Ok(json).withCookies(GuMemCookie.getAdditionCookie(json))
    }

}

object CommonActions {
  val acquisitionDataSessionKey: String = "acquisitionData"
}

abstract class OAuthActions(parser: BodyParser[AnyContent], implicit val executionContext: ExecutionContext, val authConfig: GoogleAuthConfig, commonActions: CommonActions) extends googleauth.LoginSupport with googleauth.Filters {

  import commonActions.NoCacheAction
  //routes
  private val loginTarget = routes.OAuth.loginAction()
  override val defaultRedirectTarget = routes.FrontPage.welcome()
  override val failureRedirectTarget = routes.OAuth.login()


  lazy val groupChecker = Config.googleGroupChecker

  lazy val GoogleAuthAction = new googleauth.AuthAction(authConfig, loginTarget, parser)(executionContext)
  lazy val GoogleAuthenticatedStaffAction = NoCacheAction andThen GoogleAuthAction
  lazy val permanentStaffGroups = Config.staffAuthorisedEmailGroups

  lazy val PermanentStaffNonMemberAction =
    GoogleAuthenticatedStaffAction andThen requireGroup[GoogleAuthRequest](permanentStaffGroups, unauthorisedStaff(views.html.fragments.oauth.staffUnauthorisedError())(_))

  lazy val AuthorisedStaff =
    GoogleAuthenticatedStaffAction andThen
      requireGroup[GoogleAuthRequest](permanentStaffGroups, unauthorisedStaff(views.html.fragments.oauth.staffWrongGroup())(_))

  def googleAuthenticationRefiner = {
    new ActionRefiner[AuthRequest, IdentityGoogleAuthRequest] {
      override protected implicit def executionContext: ExecutionContext = OAuthActions.this.executionContext

      override def refine[A](request: AuthRequest[A]) = Future.successful {
        userIdentity(request).map(IdentityGoogleAuthRequest(_, request)).toRight(GoogleAuthAction.sendForAuth(request))
      }
    }
  }
}
