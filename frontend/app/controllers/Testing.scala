package controllers

import com.typesafe.scalalogging.slf4j.LazyLogging
import play.api.mvc.{Cookie, Controller}
import utils.TestUsers.testUsers

object Testing extends Controller with LazyLogging {

  def testUser = GoogleAuthenticatedStaffNonMemberAction { implicit request =>
    val testUserString = testUsers.generate()
    logger.info(s"Generated test user string $testUserString for ${request.user.email}")
    Ok(views.html.testing.testUsers(testUserString)).withCookies(Cookie("ANALYTICS_OFF_KEY", "true", httpOnly = false))
  }

}
