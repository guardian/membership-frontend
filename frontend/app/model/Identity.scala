package model

// Code in this file ported over from identity-play-auth which is deprecated.

import com.gu.identity.model.cookies.{CookieDescription, CookieDescriptionList}
import play.api.mvc.Cookie
// import Writes type classes required to generate Writes instances for case classes defined in this file
import com.gu.identity.model.play.WritesInstances._
// import Reads type classes required to generate Reads instances for case classes defined in this file
import com.gu.identity.model.play.ReadsInstances._
import com.gu.identity.model.{PrivateFields, PublicFields, StatusFields}
import com.gu.identity.model.{User => IdUser}
import play.api.libs.json.{Json, Reads, Writes}

case class CreateIdUser(
    primaryEmailAddress: String,
    password: String,
    publicFields: PublicFields,
    privateFields: Option[PrivateFields] = None,
    statusFields: Option[StatusFields] = None
)

object CreateIdUser {
  implicit val writesCreateIdUser: Writes[CreateIdUser] = Json.writes[CreateIdUser]
}

case class UpdateIdUser(
    primaryEmailAddress: Option[String] = None,
    publicFields: Option[PublicFields] = None,
    privateFields: Option[PrivateFields] = None,
    statusFields: Option[StatusFields] = None
)

object UpdateIdUser {
  implicit val writesUpdateIdUser: Writes[UpdateIdUser] = Json.writes[UpdateIdUser]
}

case class UserRegistrationResult(
    user: IdUser,
    // accessToken: Option[AccessTokenDescription],
    cookies: Option[CookieDescriptionList] // `authenticate=true&format=cookies` - see https://github.com/guardian/identity/pull/621
)

object UserRegistrationResult {
  implicit val readsCookieDescriptionList: Reads[CookieDescription] = Json.reads[CookieDescription]
  implicit val readsCookieList: Reads[CookieDescriptionList] = Json.reads[CookieDescriptionList]
  implicit val readsResult: Reads[UserRegistrationResult] = Json.reads[UserRegistrationResult]
}

sealed trait AccessCredentials
object AccessCredentials {
  case class Cookies(scGuU: String, guU: Option[String] = None) extends AccessCredentials {
    val cookies: Seq[Cookie] = Seq(Cookie("SC_GU_U", scGuU)) ++ guU.map(c => Cookie("GU_U", c))
  }
  case class Token(tokenText: String) extends AccessCredentials
}
case class IdMinimalUser(id: String, displayName: Option[String])
case class AuthenticatedIdUser(credentials: AccessCredentials, minimalUser: IdMinimalUser)
