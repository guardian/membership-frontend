package services

import com.gu.identity.cookie.IdentityCookieDecoder
import com.gu.identity.model.User
import configuration.Config
import play.api.mvc.{Request, RequestHeader}

trait AuthenticationService {
  def idWebAppSigninUrl: (String => String)

  val cookieDecoder: IdentityCookieDecoder

  def authenticatedUserFor[A](request: RequestHeader): Option[User] = for {
    scGuU <- request.cookies.get("SC_GU_U")
    guU <- request.cookies.get("GU_U")
    minimalSecureUser <- cookieDecoder.getUserDataForScGuU(scGuU.value)
    guUCookieData <- cookieDecoder.getUserDataForGuU(guU.value)
    fullUser = guUCookieData.getUser if fullUser.getId == minimalSecureUser.getId
  } yield fullUser

  def requestPresentsAuthenticationCredentials(request: Request[_]) = authenticatedUserFor(request).isDefined
}

object AuthenticationService extends AuthenticationService {
  def idWebAppSigninUrl = Config.idWebAppSigninUrl

  val cookieDecoder = new IdentityCookieDecoder(Config.idKeys)
}
