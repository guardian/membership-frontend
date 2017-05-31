package utils

import play.api.mvc.{Cookie, RequestHeader}

case class ReferralData(campaignCode: Option[String], url: Option[String], pageviewId: Option[String])

object ReferralData {

  val CampaignCodeKey = "mem_campaign_code"
  val UrlKey = "gu_mem_ref_url"
  val PageviewIdKey = "gu_refpvid"

  def makeCookies(implicit request: RequestHeader): Seq[Cookie] = {
    val refUrl = request.headers.get("referer").map(Cookie(ReferralData.UrlKey, _))
    val refPvid = request.getQueryString("REFPVID").map(Cookie(ReferralData.PageviewIdKey, _))

    (refUrl ++: refPvid).toList
  }

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
