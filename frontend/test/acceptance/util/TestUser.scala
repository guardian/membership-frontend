package acceptance.util

import com.github.nscala_time.time.Imports._
import com.gu.identity.testing.usernames.TestUsernames
import org.openqa.selenium.Cookie

class TestUser {
  private val testUsers = TestUsernames(
    com.gu.identity.testing.usernames.Encoder.withSecret(Config.testUsersSecret),
    recency = 2.days.standardDuration
  )

  private def addTestUserCookies(testUsername: String) = {
    val analyticsCookie = new Cookie("ANALYTICS_OFF_KEY", "true")
    Config.driver.manage().addCookie(analyticsCookie)

    val testUserCookie = new Cookie("pre-signin-test-user", testUsername)
    Config.driver.manage().addCookie(testUserCookie)
  }

  val username = testUsers.generate()
  addTestUserCookies(username)
}
