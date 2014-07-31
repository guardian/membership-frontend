package services

import play.api.mvc.{Call, Request, Result}
import play.api.mvc.Results.SeeOther
import com.gu.identity.cookie.IdentityCookieDecoder
import com.netaporter.uri.dsl._
import actions.AuthRequest

import configuration.Config

trait AuthenticationService {
  def idWebAppSigninUrl: (String => String)

  val cookieDecoder: IdentityCookieDecoder

  def handleAuthenticatedRequest[A](request: Request[A]): Either[Result, AuthRequest[A]] = {
    authenticatedRequestFor(request).toRight {
      val chooseSigninOrRegister: Call = controllers.routes.Login.chooseSigninOrRegister(request.uri, None)
      SeeOther(chooseSigninOrRegister.absoluteURL(secure = true)(request))
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
  def idWebAppSigninUrl = Config.idWebAppSigninUrl

  val cookieDecoder = new IdentityCookieDecoder(Config.idKeys)
}
