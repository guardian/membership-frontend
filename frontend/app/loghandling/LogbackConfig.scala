package loghandling

import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.classic.{Logger => LogbackLogger, LoggerContext}
import com.amazonaws.auth.AWSCredentialsProvider
import com.gu.logback.appender.kinesis.KinesisAppender
import net.logstash.logback.layout.LogstashLayout
import org.slf4j.{Logger => SLFLogger, LoggerFactory}

object LogbackConfig {

  lazy val loggingContext = LoggerFactory.getILoggerFactory.asInstanceOf[LoggerContext]

  case class KinesisAppenderConfig(stream: String,
                                   region: String,
                                   awsCredentialsProvider: AWSCredentialsProvider,
                                   bufferSize: Int)

  def makeCustomFields(customFields: Map[String, String]): String = {
    "{" + (for((k, v) <- customFields) yield s""""${k}":"${v}"""").mkString(",") + "}"
  }

  def makeLayout(customFields: String) = {
    val l = new LogstashLayout()
    l.setCustomFields(customFields)
    l
  }

  def makeKinesisAppender(layout: LogstashLayout, context: LoggerContext, appenderConfig: KinesisAppenderConfig) = {
    val a = new KinesisAppender[ILoggingEvent]()
    a.setName("LoggingKinesisAppender")
    a.setStreamName(appenderConfig.stream)
    a.setRegion(appenderConfig.region)
    a.setCredentialsProvider(appenderConfig.awsCredentialsProvider)
    a.setBufferSize(appenderConfig.bufferSize)

    a.setContext(context)
    a.setLayout(layout)

    layout.start()
    a.start()
    a
  }

  def init(config: LogStashConf) = {
    if (config.enabled) {
      try {
        val rootLogger = LoggerFactory.getLogger(SLFLogger.ROOT_LOGGER_NAME)
        rootLogger match {
          case lb: LogbackLogger =>
            lb.info("Kinesis logging - Configuring Logback")
            val context = lb.getLoggerContext
            val layout = makeLayout(makeCustomFields(config.customFields))
            val bufferSize = 1000
            val appender = makeKinesisAppender(
              layout,
              context,
              KinesisAppenderConfig(
                config.stream,
                config.region,
                config.awsCredentialsProvider,
                bufferSize
              )
            )
            lb.addAppender(appender)
            lb.info("Kinesis logging - Configured Logback")
          case _ =>
            PlayLogger.info("Kinesis logging failed - not running using logback")
        }
      } catch {
        case ex: Throwable => PlayLogger.info(s"Kinesis logging failed with exception: $ex")
      }
    } else {
      PlayLogger.info("Kinesis logging not enabled by default (e.g. DEV mode)")
    }
  }

}
