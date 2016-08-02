package controllers

import com.gu.i18n._
import play.api.mvc._
import tracking.RedirectWithCampaignCodes._

object Giraffe extends Controller {

  def redirectToContributions() = NoCacheAction { implicit request =>
    redirectWithCampaignCodes("https://contribute.theguardian.com/", MOVED_PERMANENTLY)
  }

  def redirectToContributionsFor(countryGroup: CountryGroup) = NoCacheAction { implicit request =>
    redirectWithCampaignCodes(s"https://contribute.theguardian.com/${countryGroup.id}", MOVED_PERMANENTLY)
  }

}
