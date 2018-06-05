package monitoring

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply
import com.gu.monitoring.SafeLogger
import com.gu.monitoring.SafeLogger._
import configuration.Config
import io.sentry.Sentry
import io.sentry.logback.SentryAppender
import org.slf4j.Logger.ROOT_LOGGER_NAME
import org.slf4j.LoggerFactory

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

class PiiFilter extends Filter[ILoggingEvent] {
  override def decide(event: ILoggingEvent): FilterReply = if (event.getMarker.contains(SafeLogger.sanitizedLogMessage)) FilterReply.ACCEPT
  else FilterReply.DENY
}

object SentryLogging {

  def init(): Unit = {
    Config.sentryDsn match {
      case Failure(ex) =>
        SafeLogger.warn("No server-side Sentry logging configured (OK for dev)")
      case Success(dsn) =>
        SafeLogger.info(s"Initialising Sentry logging.")
        Try {
          val sentryClient = Sentry.init(dsn)

          val sentryAppender = new SentryAppender {
            addFilter(SentryFilters.errorLevelFilter)
            addFilter(SentryFilters.piiFilter)
          }
          sentryAppender.start()

          val buildInfo: Map[String, String] = app.BuildInfo.toMap.mapValues(_.toString)
          val tags = Map("stage" -> Config.stage.toString) ++ buildInfo
          sentryClient.setTags(tags.asJava)

          LoggerFactory.getLogger(ROOT_LOGGER_NAME).asInstanceOf[Logger].addAppender(sentryAppender)
        } match {
          case Success(_) => SafeLogger.debug("Sentry logging configured.")
          case Failure(e) => SafeLogger.error(scrub"Something went wrong when setting up Sentry logging ${e.getStackTrace}")
        }
        SafeLogger.error(scrub"*TEST* from membership-frontend. Ignore me. ")
    }
  }
}

object SentryFilters {

  val errorLevelFilter = new ThresholdFilter { setLevel("ERROR") }
  val piiFilter = new PiiFilter
  errorLevelFilter.start()
  piiFilter.start()

}

