package services

import java.net.URLEncoder
import play.api.mvc.Request
import play.api.mvc.SimpleResult
import play.api.mvc.Results.SeeOther
import com.gu.identity.cookie.{ IdentityCookieDecoder, PreProductionKeys }
import actions.AuthRequest

import com.typesafe.config.ConfigFactory

trait AuthenticationService {
  val identityWebAppUrl: String
  val cookieDecoder: IdentityCookieDecoder

  def handleAuthenticatedRequest[A](request: Request[A]): Either[SimpleResult, AuthRequest[A]] = {
    authenticatedRequestFor(request).toRight {
      val returnUrl = URLEncoder.encode(request.uri, "UTF-8")
      SeeOther(s"$identityWebAppUrl/signin?returnUrl=$returnUrl")
    }
  }

  def authenticatedRequestFor[A](request: Request[A]): Option[AuthRequest[A]] = for {
    scGuU <- request.cookies.get("SC_GU_U")
    guU <- request.cookies.get("GU_U")
    minimalSecureUser <- cookieDecoder.getUserDataForScGuU(scGuU.value)
    guUCookieData <- cookieDecoder.getUserDataForGuU(guU.value)
    fullUser = guUCookieData.getUser if fullUser.getId == minimalSecureUser.getId
  } yield AuthRequest(request, fullUser)

  def requestPresentsAuthenticationCredentials(request: Request[_]) = authenticatedRequestFor(request).isDefined
}

object AuthenticationService extends AuthenticationService {
  val config = ConfigFactory.load()
  val identityWebAppUrl = config.getString("identity.webapp.url")

  val cookieDecoder = new IdentityCookieDecoder(new PreProductionKeys)
}
