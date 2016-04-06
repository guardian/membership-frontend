package controllers

import play.api.mvc.Controller

object Login extends Controller {

  /*
   *   Interstitial sign in page =========================================
   */
  def chooseSigninOrRegister(returnUrl: String) =
    NoCacheAction { implicit request =>
      Ok(views.html.login.signin(returnUrl))
    }
}
