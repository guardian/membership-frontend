package utils

import com.gu.identity.testing.usernames.TestUsernames

import configuration.Config
import model.IdMinimalUser

object TestUsers {

  lazy val testUsers = TestUsernames(com.gu.identity.testing.usernames.Encoder.withSecret(Config.config.getString("identity.test.users.secret")))

  def isTestUser(userPrivateFields: model.PrivateFields): Boolean =
    userPrivateFields.firstName.exists(testUsers.isValid)

  def validate(user: IdMinimalUser): Boolean =
    user.displayName.exists(dn => TestUsers.testUsers.validate(dn.split(' ')(0)).isDefined)
}
