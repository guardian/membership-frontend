package monitoring

import java.lang.Long
import java.lang.System.currentTimeMillis
import com.gu.monitoring.SafeLogger
import com.typesafe.scalalogging.StrictLogging
import controllers.{Cached, NoCache}
import play.api.PlayException.ExceptionSource
import play.api._
import play.api.http.DefaultHttpErrorHandler
import play.api.mvc.Results._
import play.api.mvc._
import play.api.routing.Router
import play.core.SourceMapper
import scala.concurrent._

class ErrorHandler(
  env: Environment,
  config: Configuration,
  sourceMapper: Option[SourceMapper],
  router: => Option[Router],
  implicit val executionContext: ExecutionContext
) extends DefaultHttpErrorHandler(env, config, sourceMapper, router) with StrictLogging {

  override protected def logServerError(request: RequestHeader, usefulException: UsefulException): Unit = {
    val lineInfo = usefulException match {
      case source: ExceptionSource => s"${source.sourceName()} at line ${source.line()}"
      case _ => "unknown line number, please check the logs"
    }
    val sanitizedExceptionDetails = s"Caused by: ${usefulException.cause} in $lineInfo"
    val requestDetails = s"(${request.method}) [${request.path}]" // Use path, not uri, as query strings often contain things like ?api-key=my_secret

    // We are deliberately bypassing the SafeLogger here, because we need to use standard string interpolation to make this exception handling useful.
    logger.error(SafeLogger.sanitizedLogMessage, s"Internal server error, for $requestDetails. $sanitizedExceptionDetails")

    super.logServerError(request, usefulException) // We still want the full uri and stack trace in our logs, just not in Sentry
  }

  override def onClientError(request: RequestHeader, statusCode: Int, message: String = ""): Future[Result] = {
    super.onClientError(request, statusCode, message).map(Cached(_))
  }

  override protected def onNotFound(request: RequestHeader, message: String): Future[Result] = {
    Future.successful(Cached(NotFound(views.html.error404())))
  }

  override protected def onProdServerError(request: RequestHeader, exception: UsefulException): Future[Result] =
    Future.successful(NoCache(InternalServerError(views.html.error500(exception))))

  override protected def onBadRequest(request: RequestHeader, message: String): Future[Result] = {
    val reference = Long.toString(currentTimeMillis(), 36).toUpperCase
    logger.warn(s"A bad request was received. URI: ${request.uri}, Reference: $reference")
    Future.successful(NoCache(BadRequest(views.html.error400(request, s"Bad request received. Reference: $reference"))))
  }
}
