package controllers

import actions.CommonActions
import play.api.mvc.{BaseController, ControllerComponents}
import com.netaporter.uri.dsl._
import tracking.RedirectWithCampaignCodes.internalCampaignCode

class VanityUrl(commonActions: CommonActions, override protected val controllerComponents: ControllerComponents) extends BaseController {

  import commonActions.CachedAction

  def redirect = CachedAction { implicit request =>
    MovedPermanently(routes.FrontPage.index().url ? (internalCampaignCode -> "pap_233874"))
  }
}
