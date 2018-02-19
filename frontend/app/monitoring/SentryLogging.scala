package monitoring

import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.{Logger, LoggerContext}
import ch.qos.logback.core.filter.Filter
import ch.qos.logback.core.spi.FilterReply
import com.getsentry.raven.RavenFactory
import com.getsentry.raven.dsn.Dsn
import com.getsentry.raven.logback.SentryAppender
import com.gu.monitoring.SafeLogger
import configuration.Config
import org.slf4j.Logger.ROOT_LOGGER_NAME
import org.slf4j.LoggerFactory
import scala.util.{Failure, Success, Try}

object SentryFilters {

  class PiiFilter extends Filter[ILoggingEvent] {
    override def decide(event: ILoggingEvent): FilterReply = if (event.getMarker.contains(SafeLogger.sanitizedLogMessage)) FilterReply.ACCEPT
    else FilterReply.DENY
  }

  val errorLevelFilter = new ThresholdFilter { setLevel("ERROR") }
  val piiFilter = new PiiFilter

  errorLevelFilter.start()
  piiFilter.start()

}

object SentryLogging {

  val UserIdentityId = "userIdentityId"
  val UserGoogleId = "userGoogleId"
  val PlayErrorId = "playErrorId"
  val AllMDCTags = Seq(UserIdentityId, UserGoogleId,PlayErrorId)

  def init() {
    Try(new Dsn(Config.config.getString("sentry.dsn"))) match {
      case Failure(ex) =>
        SafeLogger.warn("No server-side Sentry logging configured (OK for dev)")
      case Success(dsn) =>
        SafeLogger.info(s"Initialising Sentry logging for ${dsn.getHost}")
        val buildInfo: Map[String, Any] = app.BuildInfo.toMap
        val tags = Map("stage" -> Config.stage) ++ buildInfo
        val tagsString = tags.map { case (key, value) => s"$key:$value"}.mkString(",")
        val sentryAppender = new SentryAppender(RavenFactory.ravenInstance(dsn)) {
          addFilter(SentryFilters.errorLevelFilter)
          addFilter(SentryFilters.piiFilter)
          setTags(tagsString)
          setRelease(app.BuildInfo.gitCommitId)
          setExtraTags(AllMDCTags.mkString(","))
          setContext(LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext])
        }
        sentryAppender.start()
        LoggerFactory.getLogger(ROOT_LOGGER_NAME).asInstanceOf[Logger].addAppender(sentryAppender)
    }
  }
}
