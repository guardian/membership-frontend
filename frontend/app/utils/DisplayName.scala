package utils

import com.gu.identity.model.User

object DisplayName {
  def apply(user: User): Option[String] = user.publicFields.username.orElse(user.privateFields.firstName)
  def fallbackFullName(user: User): Option[String] = {
    val fullName = for {
      firstName <- user.privateFields.firstName
      lastName <- user.privateFields.secondName
    } yield s"$firstName $lastName"
    user.publicFields.username.orElse(fullName)
  }
}
