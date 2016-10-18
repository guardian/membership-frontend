package utils

import play.api.mvc.RequestHeader
import play.utils.UriEncoding

case class CampaignCode(get: String)

object CampaignCode {
  def fromRequest(implicit request: RequestHeader): Option[CampaignCode] =
    for {
      cookie <- request.cookies.get("s_sess")
      decodedCookie = UriEncoding.decodePathSegment(cookie.value, "utf-8")
      regexMatch <- "campaign=(.+?);".r.findFirstMatchIn(decodedCookie)
      code <- Option(regexMatch.group(1))
    } yield CampaignCode(code)
}
