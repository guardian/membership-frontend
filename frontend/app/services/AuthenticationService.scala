package services

import play.api.mvc.{Request, RequestHeader}

import com.gu.identity.cookie.IdentityCookieDecoder

import configuration.Config
import model.IdMinimalUser

trait AuthenticationService {
  def idWebAppSigninUrl: (String => String)

  val cookieDecoder: IdentityCookieDecoder

  def authenticatedUserFor[A](request: RequestHeader): Option[IdMinimalUser] = for {
    scGuU <- request.cookies.get("SC_GU_U")
    guU <- request.cookies.get("GU_U")
    minimalSecureUser <- cookieDecoder.getUserDataForScGuU(scGuU.value)
    guUCookieData <- cookieDecoder.getUserDataForGuU(guU.value)
    user = guUCookieData.user if user.id == minimalSecureUser.id
  } yield IdMinimalUser(user.id, user.publicFields.displayName)

  def requestPresentsAuthenticationCredentials(request: Request[_]) = authenticatedUserFor(request).isDefined
}

object AuthenticationService extends AuthenticationService {
  def idWebAppSigninUrl = Config.idWebAppSigninUrl

  val cookieDecoder = new IdentityCookieDecoder(Config.idKeys)
}
