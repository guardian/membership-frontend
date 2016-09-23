package tracking

import controllers.Giraffe._
import play.api.mvc.{RequestHeader, Result}

object RedirectWithCampaignCodes {

  // GA parameters - see https://support.google.com/analytics/answer/1033863
  val GoogleAnalyticsParameters = Set(
    "utm_source", // Identify the advertiser, site, publication, etc. that is sending traffic to your property, for example: google, newsletter4, billboard.
    "utm_medium", // The advertising or marketing medium, for example: cpc, banner, email newsletter.
    "utm_campaign", // The individual campaign name, slogan, promo code, etc. for a product.
    "utm_term", // Identify paid search keywords. If you're manually tagging paid keyword campaigns, you should also use utm_term to specify the keyword.
    "utm_content" // Used to differentiate similar content, or links within the same ad. For example, if you have two call-to-action links within the same email
  )

  val CampaignCodesToForward = GoogleAnalyticsParameters ++ Set(
    "CMP_BUNIT", // helps business unit report more easily on their marketing campaigns by allowing them to filter by business unit
    "CMP_TU", // campaign team
    "INTCMP",
    "CMP",
    "mcopy"
  )

  val internalCampaignCode = "INTCMP"

  def redirectWithCampaignCodes(url: String, status: Int)(implicit request: RequestHeader): Result =
    Redirect(url, request.queryString.filterKeys(CampaignCodesToForward), status)
}
