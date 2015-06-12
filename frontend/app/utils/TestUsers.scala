package utils

import com.github.nscala_time.time.Imports._
import com.gu.identity.play.{IdMinimalUser, PrivateFields}
import com.gu.identity.testing.usernames.TestUsernames
import configuration.Config

object TestUsers {

  val ValidityPeriod = 2.days

  lazy val testUsers = TestUsernames(
    com.gu.identity.testing.usernames.Encoder.withSecret(Config.config.getString("identity.test.users.secret")),
    recency = ValidityPeriod.standardDuration
  )

  def isTestUser(user: IdMinimalUser): Boolean =
    user.displayName.flatMap(_.split(' ').headOption).exists(TestUsers.testUsers.isValid)
}
