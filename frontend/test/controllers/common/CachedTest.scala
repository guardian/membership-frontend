package controllers.common

import org.specs2.mutable.Specification
import play.api.mvc.Results
import controllers.Cached

class CachedTest extends Specification with Results {

  "Cached" should {

    "cache live content for 30 seconds" in {
      val result = Cached(2)(Ok("foo"))
      val headers = result.header.headers
      headers("Cache-Control") mustEqual "max-age=2"
    }
  }

}
