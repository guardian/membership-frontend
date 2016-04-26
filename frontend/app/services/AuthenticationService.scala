package services

import com.gu.identity.play.AccessCredentials.{Cookies, Token}
import com.gu.identity.play.AuthenticatedIdUser.Provider
import configuration.Config

object AuthenticationService extends com.gu.identity.play.AuthenticationService {
  def idWebAppSigninUrl(returnUrl: String) = Config.idWebAppSigninUrl(returnUrl)

  val identityKeys = Config.idKeys

  override lazy val authenticatedIdUserProvider: Provider =
    Cookies.authProvider(identityKeys).withDisplayNameProvider(Token.authProvider(identityKeys, "membership"))

}
