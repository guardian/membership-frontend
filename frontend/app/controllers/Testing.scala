package controllers

import com.typesafe.scalalogging.LazyLogging
import play.api.mvc.{Controller, Cookie}
import play.twirl.api.Html
import utils.TestUsers.testUsers
import actions.ActionRefiners._

object Testing extends Controller with LazyLogging {

  val AnalyticsCookieName = "ANALYTICS_OFF_KEY"

  val analyticsOffCookie = Cookie(AnalyticsCookieName, "true", httpOnly = false)

  /**
   * Make sure to use cannonical @guardian.co.uk for addresses
   */
  val AuthorisedTester = GoogleAuthenticatedStaffAction andThen isInAuthorisedGroupGoogleAuthReq(
    Set(
      "membership.dev@guardian.co.uk",
      "membership.team@guardian.co.uk",
      "dig.dev.web-engineers@guardian.co.uk",
      "membership.testusers@guardian.co.uk",
      "touchpoint@guardian.co.uk",
      "crm@guardian.co.uk"
    ),
    views.html.fragments.oauth.staffWrongGroup()
  )

  def testUser = AuthorisedTester { implicit request =>
    val testUserString = testUsers.generate()
    logger.info(s"Generated test user string $testUserString for ${request.user.email}")
    Ok(views.html.testing.testUsers(testUserString)).withCookies(analyticsOffCookie)
  }

  def analyticsOff = CachedAction {
    Ok(s"${analyticsOffCookie.name} cookie dropped").withCookies(analyticsOffCookie)
  }

}
