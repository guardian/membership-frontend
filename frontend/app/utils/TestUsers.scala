package utils

import com.gu.identity.testing.usernames.TestUsernames

import configuration.Config
import model.BasicUser

object TestUsers {

  lazy val testUsers = TestUsernames(com.gu.identity.testing.usernames.Encoder.withSecret(Config.config.getString("identity.test.users.secret")))

  def validate(user: BasicUser) =
    user.displayName.exists(dn => TestUsers.testUsers.validate(dn.split(' ')(0)).isDefined)
}
