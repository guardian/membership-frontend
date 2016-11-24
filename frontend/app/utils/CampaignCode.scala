package utils

import play.api.mvc.RequestHeader
import play.utils.UriEncoding

case class CampaignCode(get: String)

object CampaignCode {
  def fromRequest(implicit request: RequestHeader): Option[CampaignCode] =
    for {
      cookie <- request.cookies.get("mem_campaigncode")
    } yield {
    	CampaignCode(cookie.value)
    }
}
