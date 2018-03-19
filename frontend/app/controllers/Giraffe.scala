package controllers

import actions.CommonActions
import com.gu.i18n._
import play.api.mvc._
import tracking.RedirectWithCampaignCodes._

class Giraffe(commonActions: CommonActions, override protected val controllerComponents: ControllerComponents) extends BaseController {

  import commonActions.NoCacheAction

  def redirectToContributions() = NoCacheAction { implicit request =>
    Redirect("https://contribute.theguardian.com/", campaignCodes(request), MOVED_PERMANENTLY)
  }

  def redirectToContributionsFor(countryGroup: CountryGroup) = NoCacheAction { implicit request =>
    Redirect(s"https://contribute.theguardian.com/${countryGroup.id}", campaignCodes(request), MOVED_PERMANENTLY)
  }

}
