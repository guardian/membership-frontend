package filters


import akka.stream.Materializer
import play.api.mvc._

import scala.concurrent.{ExecutionContext, Future}
import controllers.Cached.suitableForCaching

class CheckCacheHeadersFilter(implicit val mat: Materializer, implicit val ec: ExecutionContext) extends Filter {

  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = {
    nextFilter(requestHeader).map { result =>
      if (suitableForCaching(result)) {
        val hasCacheControl = result.header.headers.contains("Cache-Control")
        assert(hasCacheControl, s"Cache-Control not set. Ensure controller response has Cache-Control header set for ${requestHeader.path}. Throwing exception... ")
      }
      result
    }
  }
}




