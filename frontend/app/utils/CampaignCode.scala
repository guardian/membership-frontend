package utils

import play.api.mvc.RequestHeader
import play.utils.UriEncoding

case class CampaignCode(get: String)

object CampaignCode {
  def fromRequest(implicit request: RequestHeader): Option[CampaignCode] =
    for {
      cookie <- request.cookies.get("mem_campaign_code")
    } yield {
    	CampaignCode(cookie.value)
    }
}


case class RefererUrl(get: String)

object RefererUrl {
  def fromRequest(implicit request: RequestHeader): Option[RefererUrl] =
    for {
      referer <- request.cookies.get("gu_mem_ref_url")
    } yield {
      println("referer fromRequest method: " + referer)
      RefererUrl(referer.value)
    }
}

case class RefererPageviewId(get: String)

object RefererPageviewId {
  def fromRequest(implicit request: RequestHeader): Option[RefererPageviewId] =
    for {
      cookie <- request.cookies.get("gu_refpvid")
    } yield {
      RefererPageviewId(cookie.value)
    }
}
