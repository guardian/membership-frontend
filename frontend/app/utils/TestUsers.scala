package utils

import com.gu.identity.testing.usernames.TestUsernames

import configuration.Config
import model.IdMinimalUser
import com.github.nscala_time.time.Imports._

object TestUsers {

  val ValidityPeriod = 2.days

  lazy val testUsers = TestUsernames(
    com.gu.identity.testing.usernames.Encoder.withSecret(Config.config.getString("identity.test.users.secret")),
    recency = ValidityPeriod.standardDuration
  )

  def isTestUser(userPrivateFields: model.PrivateFields): Boolean =
    userPrivateFields.firstName.exists(testUsers.isValid)

  def validate(user: IdMinimalUser): Boolean =
    user.displayName.exists(dn => TestUsers.testUsers.validate(dn.split(' ')(0)).isDefined)
}
