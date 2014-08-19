package controllers

import configuration.Config
import play.api.mvc.{Request, RequestHeader}


object IdentityHeaders extends RemoteAddress {
  def apply(request: Request[_]): List[(String, String)] = {
    val cookie = request.cookies.get("SC_GU_U")
    val ipAddress = clientIp(request)

    List(("X-GU-ID-Client-Access-Token" -> s"Bearer ${Config.idApiClientToken}")) ++
      cookie.map("X-GU-ID-FOWARDED-SC-GU-U" -> _.value) ++
      request.headers.get("Referer").map(("trackingReferer" -> _)) ++
      request.headers.get("User-Agent").map(("trackingUserAgent" -> _)) ++
      ipAddress.map(("trackingIpAddress" -> _))

  }
}

//Copying NGW on how to pass the IP across to Identity API
trait RemoteAddress {
  private val Ip = """(\d{1,3})\.(\d{1,3})\.(\d{1,3})\.(\d{1,3})""".r

  def clientIp(request: RequestHeader): Option[String] = {
    request.headers.get("X-Forwarded-For").flatMap { xForwardedFor =>
      xForwardedFor.split(", ").find {
        // leftmost non-private IP from header is client
        case Ip(a, b, c, d) => {
          if ("10" == a) false
          else if ("192" == a && "168" == b) false
          else if ("172" == a && (16 to 31).map(_.toString).contains(b)) false
          else true
        }
        case _ => false
      }
    }
  }
}