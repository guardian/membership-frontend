package filters

import play.api.mvc._
import scala.concurrent.Future
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object CheckCacheHeadersFilter extends Filter {

  def apply(nextFilter: RequestHeader => Future[SimpleResult])(requestHeader: RequestHeader): Future[SimpleResult] = {
    nextFilter(requestHeader).map { result =>
      val hasCacheControl = result.header.headers.contains("Cache-Control")
      assert(hasCacheControl, "Cache-Control not set. Ensure any modified controllers have Cache-Control header set. Throwing exception... ")
      result
    }
  }
}