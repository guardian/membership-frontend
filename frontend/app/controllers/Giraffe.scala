package controllers

import com.gu.i18n._
import play.api.mvc._
import tracking.RedirectWithCampaignCodes._
import javax.inject.{Inject, Singleton}

@Singleton
class Giraffe @Inject()() extends Controller {

  def redirectToContributions() = NoCacheAction { implicit request =>
    Redirect("https://contribute.theguardian.com/", campaignCodes(request), MOVED_PERMANENTLY)
  }

  def redirectToContributionsFor(countryGroup: CountryGroup) = NoCacheAction { implicit request =>
    Redirect(s"https://contribute.theguardian.com/${countryGroup.id}", campaignCodes(request), MOVED_PERMANENTLY)
  }

}
