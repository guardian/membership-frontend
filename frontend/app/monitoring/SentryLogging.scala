package monitoring

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply
import com.gu.monitoring.SafeLogger
import com.gu.monitoring.SafeLogger._
import configuration.Config
import io.sentry.Sentry
import scala.collection.JavaConverters._

import scala.util.{Failure, Success, Try}

class PiiFilter extends Filter[ILoggingEvent] {
  override def decide(event: ILoggingEvent): FilterReply = if (event.getMarker.contains(SafeLogger.sanitizedLogMessage)) FilterReply.ACCEPT
  else FilterReply.DENY
}

object SentryLogging {

  def init() = {
    Config.sentryDsn match {
      case Failure(ex) =>
        SafeLogger.warn("No server-side Sentry logging configured (OK for dev)")
      case Success(dsn) =>
        SafeLogger.info(s"Initialising Sentry logging.")
        Try {
          val sentryClient = Sentry.init(dsn)
          val buildInfo: Map[String, String] = app.BuildInfo.toMap.mapValues(_.toString)
          val tags = Map("stage" -> Config.stage.toString) ++ buildInfo
          sentryClient.setTags(tags.asJava)
        } match {
          case Success(_) => SafeLogger.debug("Sentry logging configured.")
          case Failure(e) => SafeLogger.error(scrub"Something went wrong when setting up Sentry logging ${e.getStackTrace}")
        }
    }
  }
}

