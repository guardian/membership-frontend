package actions

import org.specs2.mutable.Specification
import play.mvc.Http.RequestBuilder

class RegistrationUriTest extends Specification {

  "from request" should {
    "contain correct identity tracking code for home page" in {
      assertCodeFor("MEM_HOME_SUP", "/join/supporter/enter-details", "http://mem.thegulocal.com/")
      assertCodeFor("MEM_HOME_PAR", "/join/partner/enter-details", "http://mem.thegulocal.com/")
      assertCodeFor("MEM_HOME_PAT", "/join/patron/enter-details", "http://mem.thegulocal.com/")
      assertCodeFor("MEM_HOME_FRI", "/join/friend/enter-details", "http://mem.thegulocal.com/")
    }

    "contain correct identity tracking code for events page" in {
      assertCodeFor("MEM_EVT_SUP", "/join/supporter/enter-details", "https://membership.theguardian.com/events")
      assertCodeFor("MEM_EVT_PAR", "/join/partner/enter-details", "https://membership.theguardian.com/events")
      assertCodeFor("MEM_EVT_PAT", "/join/patron/enter-details", "https://membership.theguardian.com/events")
      assertCodeFor("MEM_EVT_FRI", "/join/friend/enter-details", "https://membership.theguardian.com/events")
    }

    "contain correct identity tracking code for event details page" in {
      assertCodeFor("MEM_COMP_SUP", "/join/supporter/enter-details", "https://membership.theguardian.com/offers-competitions")
      assertCodeFor("MEM_COMP_PAR", "/join/partner/enter-details", "https://membership.theguardian.com/offers-competitions")
      assertCodeFor("MEM_COMP_PAT", "/join/patron/enter-details", "https://membership.theguardian.com/offers-competitions")
      assertCodeFor("MEM_COMP_FRI", "/join/friend/enter-details", "https://membership.theguardian.com/offers-competitions")
    }

    "contain correct identity tracking code for US supporters" in {
      assertCodeFor("MEM_SUPUS_SUP", "/join/supporter/enter-details", "https://membership.theguardian.com/us/supporter")
      assertCodeFor("MEM_SUPUS_PAR", "/join/partner/enter-details", "https://membership.theguardian.com/us/supporter")
      assertCodeFor("MEM_SUPUS_PAT", "/join/patron/enter-details", "https://membership.theguardian.com/us/supporter")
      assertCodeFor("MEM_SUPUS_FRI", "/join/friend/enter-details", "https://membership.theguardian.com/us/supporter")
    }


    "contain correct identity tracking code for US supporters" in {
      assertCodeFor("MEM_SUPUK_SUP", "/join/supporter/enter-details", "https://membership.theguardian.com/uk/supporter")
      assertCodeFor("MEM_SUPUK_PAR", "/join/partner/enter-details", "https://membership.theguardian.com/uk/supporter")
      assertCodeFor("MEM_SUPUK_PAT", "/join/patron/enter-details", "https://membership.theguardian.com/uk/supporter")
      assertCodeFor("MEM_SUPUK_FRI", "/join/friend/enter-details", "https://membership.theguardian.com/uk/supporter")
    }


    "missing referer will return limited code" in {
      val request = new RequestBuilder().path("/join/partner/enter-details").build()._underlyingHeader()
      val regUri: String = RegistrationUri.parse(request)
      regUri must contain("MEM")

    }

  }


  private def assertCodeFor(code: String, path: String, referrer: String) =  {
    val request = new RequestBuilder().path(path).header("referer", referrer).build()._underlyingHeader()
    val regUri: String = RegistrationUri.parse(request)
    regUri must contain(code)
  }

}
