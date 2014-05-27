package services

import java.net.URLEncoder
import play.api.mvc.Request
import play.api.mvc.SimpleResult
import play.api.mvc.Results.SeeOther
import com.gu.identity.cookie.{ IdentityCookieDecoder, PreProductionKeys }
import actions.AuthRequest

import com.typesafe.config.ConfigFactory
import configuration.Config

trait AuthenticationService {
  val idWebAppUrl: String
  val membershipUrl: String

  val cookieDecoder: IdentityCookieDecoder

  def handleAuthenticatedRequest[A](request: Request[A]): Either[SimpleResult, AuthRequest[A]] = {
    authenticatedRequestFor(request).toRight {
      val returnUrl = URLEncoder.encode(s"$membershipUrl${request.uri}", "UTF-8")
      SeeOther(s"$idWebAppUrl/signin?returnUrl=$returnUrl")
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
  val membershipUrl = Config.membershipUrl
  val idWebAppUrl = Config.idWebAppUrl

  val cookieDecoder = new IdentityCookieDecoder(Config.idKeys)
}
