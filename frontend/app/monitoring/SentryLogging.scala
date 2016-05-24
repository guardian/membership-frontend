package monitoring

import ch.qos.logback.classic.filter.ThresholdFilter
import ch.qos.logback.classic.{Logger, LoggerContext}
import configuration.Config
import com.getsentry.raven.RavenFactory
import com.getsentry.raven.logback.SentryAppender
import org.slf4j.Logger.ROOT_LOGGER_NAME
import org.slf4j.LoggerFactory

import scala.util.{Failure, Success}

object SentryLogging {

  val UserIdentityId = "userIdentityId"
  val UserGoogleId = "userGoogleId"
  val AllMDCTags = Seq(UserIdentityId, UserGoogleId)

  val ravenOpt = Config.sentryDsn.map(RavenFactory.ravenInstance).toOption

  def init() {
    ravenOpt match {
      case None =>
        play.api.Logger.warn("No Sentry logging configured (OK for dev)")
      case Some(raven) =>
        play.api.Logger.info(s"Initialising Sentry logging")
        val buildInfo: Map[String, Any] = app.BuildInfo.toMap
        val tags = Map("stage" -> Config.stage) ++ buildInfo
        val tagsString = tags.map { case (key, value) => s"$key:$value"}.mkString(",")

        val filter = new ThresholdFilter { setLevel("ERROR") }
        filter.start() // OMG WHY IS THIS NECESSARY LOGBACK?

        val sentryAppender = new SentryAppender(raven) {
          addFilter(filter)
          setTags(tagsString)
          setExtraTags(AllMDCTags.mkString(","))
          setContext(LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext])
        }
        sentryAppender.start()
        LoggerFactory.getLogger(ROOT_LOGGER_NAME).asInstanceOf[Logger].addAppender(sentryAppender)
    }
  }
}
