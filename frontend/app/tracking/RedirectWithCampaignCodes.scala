package tracking

import controllers.Giraffe._
import play.api.mvc.{RequestHeader, Result}

object RedirectWithCampaignCodes {

  val CampaignCodesToForward = Set("INTCMP", "CMP", "mcopy")

  def redirectWithCampaignCodes(url: String, status: Int)(implicit request: RequestHeader): Result =
    Redirect(url, request.queryString.filterKeys(CampaignCodesToForward), status)
}
