package controllers

import actions.CommonActions
import play.api.mvc.Controller

class Login(commonActions: CommonActions) extends Controller {

  import commonActions.NoCacheAction

  /*
   *   Interstitial sign in page =========================================
   */
  def chooseSigninOrRegister(returnUrl: String) = NoCacheAction { implicit request =>
    Ok(views.html.login.signin(returnUrl))
  }

}
