package controllers

import actions.CommonActions
import play.api.mvc.{BaseController, ControllerComponents}

class Login(commonActions: CommonActions, override protected val controllerComponents: ControllerComponents) extends BaseController {

  import commonActions.NoCacheAction

  /*
   *   Interstitial sign in page =========================================
   */
  def chooseSigninOrRegister(returnUrl: String) = NoCacheAction { implicit request =>
    Ok(views.html.login.signin(returnUrl))
  }

}
