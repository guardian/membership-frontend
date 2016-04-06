package monitoring

import javax.inject._

import com.gu.googleauth.UserIdentity
import controllers.{Cached, NoCache}
import monitoring.SentryLogging.{UserGoogleId, UserIdentityId}
import org.slf4j.MDC
import play.api._
import play.api.http.DefaultHttpErrorHandler
import play.api.mvc.Results._
import play.api.mvc._
import play.api.routing.Router
import services.AuthenticationService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

class ErrorHandler @Inject()(
    env: Environment,
    config: Configuration,
    sourceMapper: OptionalSourceMapper,
    router: Provider[Router]
    ) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) {

  override def logServerError(
      request: RequestHeader, usefulException: UsefulException) {
    try {
      for (identityUser <- AuthenticationService.authenticatedUserFor(request)) {
        MDC.put(UserIdentityId, identityUser.id)
      }
      for (googleUser <- UserIdentity.fromRequest(request)) {
        MDC.put(UserGoogleId, googleUser.email.split('@').head)
      }

      super.logServerError(request, usefulException)
    } finally MDC.clear()
  }

  override def onClientError(request: RequestHeader,
                             statusCode: Int,
                             message: String = ""): Future[Result] = {
    super.onClientError(request, statusCode, message).map(Cached(_))
  }

  override protected def onNotFound(
      request: RequestHeader, message: String): Future[Result] = {
    Future.successful(Cached(NotFound(views.html.error404())))
  }

  override protected def onProdServerError(
      request: RequestHeader, exception: UsefulException): Future[Result] =
    Future.successful(
        NoCache(InternalServerError(views.html.error500(exception))))
}
