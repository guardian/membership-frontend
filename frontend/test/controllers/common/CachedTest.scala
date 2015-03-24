package controllers.common

import org.specs2.mutable.Specification
import play.api.mvc.{Result, Results}
import controllers.Cached
import org.joda.time.DateTime

class CachedTest extends Specification with Results {

  "Cached" should {
    def cacheControlSectionsOn(result: Result): Seq[String] =
      result.header.headers("Cache-Control").split(",").toSeq.map(_.trim)

    "cache live content for the specified number of seconds" in {
      cacheControlSectionsOn(Cached(2)(Ok)) must contain("max-age=2")
    }
    "serve stale content on error for 10 days if necessary" in {
      cacheControlSectionsOn(Cached(Ok)) must contain("stale-if-error=864000")
    }
  }

  "Convert to http date string" in {
    val theDate = new DateTime(2001, 5, 20, 12, 3, 4, 555)
    Cached.toHttpDateTimeString(theDate) mustEqual "Sun, 20 May 2001 11:03:04 GMT"
  }

}
