package utils

import com.gu.identity.model.{PrivateFields, PublicFields, User}
import org.scalatest.{FlatSpec, Matchers}

class DisplayNameTest extends FlatSpec with Matchers {
  val username = "username"
  val firstName = "firstName"
  val secondName = "secondName"

  "apply" should "create a display name using an Identity user's username" in {
    val user = User(
      "email@email.com",
      "userId",
      publicFields = PublicFields(username = Some(username)),
      privateFields = PrivateFields(firstName = Some(firstName), secondName = Some(secondName))
    )
    DisplayName(user) should be(Some(username))
  }
  "apply" should "fallback to first name" in {
    val user = User(
      "email@email.com",
      "userId",
      publicFields = PublicFields(username = None),
      privateFields = PrivateFields(firstName = Some(firstName), secondName = Some(secondName))
    )
    DisplayName(user) should be(Some(firstName))
  }
  "fallbackFullName" should "default to username" in {
    val user = User(
      "email@email.com",
      "userId",
      publicFields = PublicFields(username = Some(username)),
      privateFields = PrivateFields(firstName = Some(firstName), secondName = Some(secondName))
    )
    DisplayName.fallbackFullName(user) should be(Some(username))
  }
  "fallbackFullName" should "fallback to first name and last name" in {
    val user = User(
      "email@email.com",
      "userId",
      publicFields = PublicFields(username = None),
      privateFields = PrivateFields(firstName = Some(firstName), secondName = Some(secondName))
    )
    DisplayName.fallbackFullName(user) should be(Some("firstName secondName"))
  }
}
