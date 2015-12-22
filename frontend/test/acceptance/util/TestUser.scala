package acceptance.util

import com.github.nscala_time.time.Imports._
import com.gu.identity.testing.usernames.TestUsernames

class TestUser {
  private val testUsers = TestUsernames(
    com.gu.identity.testing.usernames.Encoder.withSecret(Config.testUsersSecret),
    recency = 2.days.standardDuration
  )

  private def addTestUserCookies(testUsername: String) = {
    Driver.addCookie("ANALYTICS_OFF_KEY", "true")
    Driver.addCookie("pre-signin-test-user", testUsername)
  }

  val username = testUsers.generate()
  addTestUserCookies(username)
}
