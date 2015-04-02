import configuration.Config
import controllers.Cached
import filters.{AddEC2InstanceHeader, CheckCacheHeadersFilter, Gzipper}
import monitoring.SentryLogging
import play.api.Application
import play.api.mvc.Results.{InternalServerError, NotFound}
import play.api.mvc.{RequestHeader, Result, WithFilters}
import play.filters.csrf._
import services._

import scala.concurrent.Future

object Global extends WithFilters(CheckCacheHeadersFilter, CacheSensitiveCSRFFilter(), Gzipper, AddEC2InstanceHeader) {
  override def onStart(app: Application) {
    SentryLogging.init()

    GuardianLiveEventService.start()
    LocalEventService.start()
    MasterclassEventService.start()

    TouchpointBackend.All.foreach(_.start())
    GuardianContentService.start()
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
