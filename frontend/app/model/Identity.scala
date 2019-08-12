package model

// Code in this file ported over from identity-play-auth which is deprecated.

import java.time.{Clock, Duration}

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

// identity-play-auth (which previously provided CookieBuilder) has been deprecated in favour of identity-auth-play:
// - old library: https://github.com/guardian/identity-play-auth
// - new library: https://github.com/guardian/identity/pull/1571
//
// As part of this migration, only the key features of identity-play-auth were kept
// i.e. authenticating a user from a Play RequestHeader
//
// Therefore, to preserve the CookieBuilder functionality,
// the code has been copy and pasted from the old identity-play-auth.
object CookieBuilder {

  def cookiesFromDescription(
    // Description of the cookies to set, as specified by the identity API.
    cookieDescriptionList: CookieDescriptionList,
    domain: Option[String] = None
  )(implicit clock: Clock = Clock.systemUTC()): Seq[Cookie] = {

    val maxAge = Duration.between(clock.instant(), cookieDescriptionList.expiresAt).getSeconds.toInt

    for (cookieDescription <- cookieDescriptionList.values) yield {
      val isSecure = cookieDescription.key.startsWith("SC_")
      val maxAgeOpt = if (cookieDescription.sessionCookie.contains(true)) None else Some(maxAge)
      Cookie(
        cookieDescription.key,
        cookieDescription.value,
        maxAge = maxAgeOpt,
        secure = true, // as of https://github.com/guardian/identity-frontend/pull/196
        httpOnly = isSecure, // ideally this would come from the Cookie Description
        domain = domain)
    }
  }
}
