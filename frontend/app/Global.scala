import play.api.{UsefulException, Logger, Application}
import play.api.mvc.Results.{InternalServerError, NotFound}
import play.api.mvc.{RequestHeader, Result, WithFilters}
import play.filters.csrf._

import configuration.Config
import controllers.Cached
import filters.{CheckCacheHeadersFilter, Gzipper}
import monitoring.SentryLogging
import services._

import scala.concurrent.Future

object Global extends WithFilters(CheckCacheHeadersFilter, CacheSensitiveCSRFFilter(), Gzipper) {
  override def onStart(app: Application) {
    SentryLogging.init()

    GuardianLiveEventService.start()
    MasterclassEventService.start()

    TouchpointBackend.All.foreach(_.start())
    MasterclassDataService.start()
  }

  override def onHandlerNotFound(request: RequestHeader): Future[Result] = {
    Future.successful(Cached(NotFound(views.html.error404())))
  }

  override def onError(request: RequestHeader, ex: Throwable) = {
    // Try to associate the error with a user
    AuthenticationService.authenticatedUserFor(request).foreach { user =>
      val code = ex match {
        case err: UsefulException => "@" + err.id
        case _ => "Unknown"
      }

      Logger.error(s"$code affected user ${user.id}")
    }

    if (Config.stage == "PROD") {
      Future.successful(Cached(InternalServerError(views.html.error500(ex))))
    } else {
      throw ex
    }
  }

}
