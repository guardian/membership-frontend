import scala.concurrent.Future

import play.api.mvc.{Result, RequestHeader, WithFilters}
import play.api.mvc.Results.{NotFound, InternalServerError}
import play.api.mvc.WithFilters
import play.api.Application

import controllers.Cached
import filters.{CheckCacheHeadersFilter, Gzipper}
import services.{SubscriptionService, MemberRepository, EventbriteService}
import play.filters.csrf._
import configuration.Config

object Global extends WithFilters(CheckCacheHeadersFilter, CacheSensitiveCSRFFilter(), Gzipper) {
  override def onStart(app: Application) {
    EventbriteService.start()
    MemberRepository.start()
    SubscriptionService.start()
  }

  override def onHandlerNotFound(request: RequestHeader): Future[Result] = {
    Future.successful(Cached(NotFound(views.html.error404())))
  }

  override def onError(request: RequestHeader, ex: Throwable) = {
    if (Config.stage == "PROD") {
      Future.successful(Cached(InternalServerError(views.html.error500(ex))))
    } else {
      throw ex
    }
  }

}
