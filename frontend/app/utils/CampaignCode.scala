package utils

import play.api.mvc.{Cookie, Request}
import play.utils.UriEncoding

object CampaignCode {

  def extractCampaignCode(request: Request[_]): Option[String] = {
    request.cookies.get("s_sess").flatMap{ cookie: Cookie =>
      val cookieVal = UriEncoding.decodePathSegment(cookie.value, "utf-8")
      "campaign=(.+?);".r.findFirstMatchIn(cookieVal).map(_.group(1))
    }
  }

}
