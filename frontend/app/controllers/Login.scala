package controllers

import com.gu.membership.salesforce.Tier
import com.gu.membership.salesforce.Tier
import play.api.mvc.Controller
import play.api.data.Form
import play.api.data.Forms._
import services.AuthenticationService
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object Login extends Controller {

  /*
   *   Interstitial sign in page =========================================
   */
  def chooseSigninOrRegister(returnUrl: String, tier: Option[String]) = NoCacheAction { implicit request =>
    val tierOpt: Option[Tier.Value] = tier.map(tierString => Tier.routeMap(tierString))
    Ok(views.html.login.signin(returnUrl, tierOpt))
  }

}
