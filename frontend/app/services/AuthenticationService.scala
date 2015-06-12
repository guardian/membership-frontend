package services

import configuration.Config

object AuthenticationService extends com.gu.identity.play.AuthenticationService {
  def idWebAppSigninUrl(returnUrl: String) = Config.idWebAppSigninUrl(returnUrl)

  val identityKeys = Config.idKeys
}
