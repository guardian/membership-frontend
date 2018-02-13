package controllers

import actions.CommonActions
import play.api.mvc.Controller
import com.netaporter.uri.dsl._
import tracking.RedirectWithCampaignCodes.internalCampaignCode

class VanityUrl(commonActions: CommonActions) extends Controller {

  import commonActions.CachedAction

  def redirect = CachedAction { implicit request =>
    MovedPermanently(routes.FrontPage.index().url ? (internalCampaignCode -> "pap_233874"))
  }
}
