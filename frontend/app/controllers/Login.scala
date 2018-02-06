package controllers

import play.api.mvc.Controller
import javax.inject.{Inject, Singleton}

class Login @Inject()() extends Controller {

  /*
   *   Interstitial sign in page =========================================
   */
  def chooseSigninOrRegister(returnUrl: String) = NoCacheAction { implicit request =>
    Ok(views.html.login.signin(returnUrl))
  }

}
