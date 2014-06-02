package controllers.common

import org.specs2.mutable.Specification
import play.api.mvc.Results
import controllers.Cached
import org.joda.time.DateTime

class CachedTest extends Specification with Results {

  "Cached" should {

    "cache live content for 30 seconds" in {
      val result = Cached(2)(Ok("foo"))
      val headers = result.header.headers
      headers("Cache-Control") mustEqual "max-age=2"
    }
  }

  "Convert to http date string" in {
    val theDate = new DateTime(2001, 5, 20, 12, 3, 4, 555)
    Cached.toHttpDateTimeString(theDate) mustEqual "Sun, 20 May 2001 11:03:04 GMT"
  }

}
