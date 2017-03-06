package services

import com.gu.memsub.promo.PromoCode
import org.specs2.mutable.Specification
import play.api.mvc.Cookie
import play.api.test.FakeRequest
import scalaz.syntax.std.option._
import services.PromoSessionService._

class PromoSessionServiceTest extends Specification {

  val code = PromoCode("free cats")
  val request = FakeRequest()

  "Promo session service" should {

    "Provide a session cookie for a given promocode" in {
      sessionCookieFromCode(PromoCode("free cats")) mustEqual Cookie(cookieName,  base64encode(code.get), None)
    }

    "Give you no promo codes if there are no valid cookies" in {
      codeFromSession(request.withCookies(Cookie("foo", base64encode(code.get)))) mustEqual None
      codeFromSession(request) mustEqual None
    }

    "Give you a promo code if there is a valid cookie" in {
      codeFromSession(request.withCookies(Cookie("foo", "bar"), Cookie(cookieName, base64encode(code.get)))) mustEqual code.some
    }
  }
}
