package services

import java.util.Base64
import java.nio.charset.StandardCharsets
import com.gu.memsub.promo.PromoCode
import play.api.mvc.{AnyContent, Request, Cookie}

object PromoSessionService {

  val cookieName = "GU_MEM_PROMO_CODE"

  def base64encode(str: String) =
    Base64.getEncoder.encodeToString(str.getBytes(StandardCharsets.UTF_8))

  def base64decode(str: String) = new String(Base64.getDecoder.decode(str))

  def sessionCookieFromCode(code: PromoCode) =
    Cookie(cookieName, base64encode(code.get))

  def codeFromSession(implicit request: Request[AnyContent]) =
    request.cookies
      .find(_.name == cookieName)
      .map(a => PromoCode(base64decode(a.value)))
}
