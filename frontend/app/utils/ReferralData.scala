package utils

import play.api.mvc.RequestHeader

case class ReferralData(campaignCode: Option[String], url: Option[String], pageviewId: Option[String])

object ReferralData {

  val CampaignCodeKey = "mem_campaign_code"
  val UrlKey = "gu_mem_ref_url"
  val PageviewIdKey = "gu_refpvid"

  def fromRequest(implicit request: RequestHeader): ReferralData = {
      def getCookieVal(key: String): Option[String] = {
        request.cookies.get(key).map(_.value)
      }
      ReferralData(
        getCookieVal(CampaignCodeKey),
        getCookieVal(UrlKey),
        getCookieVal(PageviewIdKey)
      )
  }
}

