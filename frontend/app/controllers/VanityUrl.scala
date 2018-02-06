package controllers

import play.api.mvc.Controller
import com.netaporter.uri.dsl._
import tracking.RedirectWithCampaignCodes.internalCampaignCode
import javax.inject.{Inject, Singleton}

@Singleton
class VanityUrl @Inject()() extends Controller {

  def redirect = CachedAction { implicit request =>
    MovedPermanently(routes.FrontPage.index().url ? (internalCampaignCode -> "pap_233874"))
  }
}
