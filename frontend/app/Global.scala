import scala.concurrent.Future

import play.api.mvc.{Result, RequestHeader, WithFilters}
import play.api.mvc.Results.NotFound
import play.api.mvc.WithFilters
import play.api.Application

import controllers.Cached
import filters.{CheckCacheHeadersFilter, Gzipper}
import services.{SubscriptionService, MemberRepository, EventbriteService}
import play.filters.csrf._

object Global extends WithFilters(CheckCacheHeadersFilter, CacheSensitiveCSRFFilter(), Gzipper) {
  override def onStart(app: Application) {
    EventbriteService.start()
    MemberRepository.start()
    SubscriptionService.zuora.start()
  }

  override def onHandlerNotFound(request: RequestHeader): Future[Result] = {
    Future.successful(Cached(NotFound(views.html.error404())))
  }
}
