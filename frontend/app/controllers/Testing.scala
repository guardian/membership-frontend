package controllers

import actions.Fallbacks._
import actions._
import com.gu.googleauth.GoogleAuthConfig
import com.typesafe.scalalogging.LazyLogging
import play.api.mvc._
import utils.TestUsers.testUsers
import play.api.libs.ws.WSClient

import scala.concurrent.ExecutionContext

class Testing(
  override val wsClient: WSClient,
  parser: BodyParser[AnyContent],
  override implicit val executionContext: ExecutionContext,
  googleAuthConfig: GoogleAuthConfig,
  commonActions: CommonActions,
  override protected val controllerComponents: ControllerComponents
) extends OAuthActions(parser, executionContext, googleAuthConfig, commonActions) with BaseController with LazyLogging {

  import commonActions.CachedAction

  import Testing._
  val analyticsOffCookie = Cookie(AnalyticsCookieName, "true", httpOnly = false)

  /**
   * Make sure to use canonical @guardian.co.uk for addresses
   */
  val AuthorisedTester = GoogleAuthenticatedStaffAction andThen requireGroup[GoogleAuthRequest](Set(
    "membership.dev@guardian.co.uk",
    "mobile.core@guardian.co.uk",
    "membership.team@guardian.co.uk",
    "dig.dev.web-engineers@guardian.co.uk",
    "membership.testusers@guardian.co.uk",
    "touchpoint@guardian.co.uk",
    "crm@guardian.co.uk",
    "dig.qa@guardian.co.uk",
    "identitydev@guardian.co.uk"
  ), unauthorisedStaff(views.html.fragments.oauth.staffWrongGroup())(_))

  def testUser = AuthorisedTester { implicit request =>
    val testUserString = testUsers.generate()
    logger.info(s"Generated test user string $testUserString for ${request.user.email}")
    val testUserCookie = Cookie(PreSigninTestCookieName, testUserString, Some(30 * 60), httpOnly = true)
    Ok(views.html.testing.testUsers(testUserString)).withCookies(testUserCookie, analyticsOffCookie)
  }

  def analyticsOff = CachedAction {
    Ok(s"${analyticsOffCookie.name} cookie dropped").withCookies(analyticsOffCookie)
  }

}

object Testing {
  val AnalyticsCookieName = "ANALYTICS_OFF_KEY"

  val PreSigninTestCookieName = "pre-signin-test-user"
}
