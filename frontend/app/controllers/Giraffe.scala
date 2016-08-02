package controllers

import com.gu.i18n._
import play.api.mvc._

object Giraffe extends Controller {

  val CampaignCodesToForward = Set("INT", "CMP", "mcopy")

  def redirectWithCampaignCodes(contributionsUrl: String)(implicit request: RequestHeader): Result = {
    Redirect(contributionsUrl, request.queryString.filterKeys(CampaignCodesToForward), MOVED_PERMANENTLY)
  }

  def redirectToContributions() = NoCacheAction { implicit request =>
    redirectWithCampaignCodes("https://contribute.theguardian.com/")
  }

  def redirectToContributionsFor(countryGroup: CountryGroup) = NoCacheAction { implicit request =>
    redirectWithCampaignCodes(s"https://contribute.theguardian.com/${countryGroup.id}")
  }

}
