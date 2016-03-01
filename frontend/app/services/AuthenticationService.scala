package services

import com.gu.identity.play.{AccessCredentials, AuthenticatedIdUser}
import configuration.Config
import play.api.mvc.RequestHeader

object AuthenticationService extends com.gu.identity.play.AuthenticationService {
  def idWebAppSigninUrl(returnUrl: String) = Config.idWebAppSigninUrl(returnUrl)

  val identityKeys = Config.idKeys

  override lazy val authenticatedIdUserProvider: (RequestHeader) => Option[AuthenticatedIdUser] = AuthenticatedIdUser.provider(
    AccessCredentials.Cookies.authProvider(identityKeys),
    AccessCredentials.Token.authProvider(identityKeys, "membership")
  )

}
