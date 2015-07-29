package services.zuora

import com.gu.membership.util.Timing
import com.gu.membership.zuora.ZuoraApiConfig
import com.gu.monitoring.{AuthenticationMetrics, StatusMetrics}
import com.typesafe.scalalogging.LazyLogging
import model.FeatureChoice
import model.Zuora._
import model.ZuoraDeserializer._
import model.ZuoraReaders._
import monitoring.TouchpointBackendMetrics
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logger
import play.api.Play.current
import play.api.libs.ws.WS
import utils.ScheduledTask

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

case class ZuoraServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}

object ZuoraServiceHelpers {
  def formatDateTime(dt: DateTime): String = {
    val str = ISODateTimeFormat.dateTime().print(dt.withZone(DateTimeZone.UTC))
    // Zuora doesn't accept Z for timezone
    str.replace("Z", "+00:00")
  }

  def formatQuery[T <: ZuoraQuery](reader: ZuoraQueryReader[T], where: String) =
    s"SELECT ${reader.fields.mkString(",")} FROM ${reader.table} WHERE $where"
}

class ZuoraService(val apiConfig: ZuoraApiConfig) extends LazyLogging {
  import services.zuora.ZuoraServiceHelpers._

  val metrics = new TouchpointBackendMetrics with StatusMetrics with AuthenticationMetrics {
    val backendEnv = apiConfig.envName

    val service = "Zuora"

    def recordError {
      put("error-count", 1)
    }
  }

  val featuresTask = ScheduledTask(s"Zuora ${apiConfig.envName} retrieve features of membership tiers", Set[Feature](), 0.seconds, 1.day) {
    val featuresF = query[Feature]("Status = 'Active'").map(_.toSet)

    featuresF.foreach { features =>
      val diff = FeatureChoice.codes &~ features.map(_.code)
      lazy val msg =
        s"""
           |Zuora ${apiConfig.envName} is missing the following product features:
           |${diff.mkString(", ")}. Please update configuration ASAP!"""
          .stripMargin

      if (diff.nonEmpty) logger.error(msg)
    }

    featuresF
  }

  lazy val featuresSchedule = featuresTask.start()

  def authTaskWithCallbacks(callbacks: Seq[() => Unit]) =
    ScheduledTask(s"Zuora ${apiConfig.envName} auth", Authentication("", ""), 0.seconds, 30.minutes) {
      val eventualAuthentication = request(Login(apiConfig))
      eventualAuthentication.filter(_.token.nonEmpty).foreach(_ => callbacks.foreach(_()))
      eventualAuthentication
    }

  val authTask = authTaskWithCallbacks(Seq(() => featuresSchedule))

  val pingTask = ScheduledTask(s"Zuora ${apiConfig.envName} ping", DateTime.now, 30.seconds, 30.seconds) {
    request(Query("SELECT Id FROM Product")).map { _ => new DateTime }
  }

  def start() {
    authTask.start()
    pingTask.start()
  }

  implicit def authentication: Authentication = authTask.get()

  def request[T <: ZuoraResult](action: ZuoraAction[T])(implicit reader: ZuoraReader[T]): Future[T] = {
    val url = if (action.authRequired) authentication.url else apiConfig.url

    if (action.authRequired && authentication.url.length == 0) {
      metrics.putAuthenticationError
      throw ZuoraServiceError(s"Can't build authenticated request for ${action.getClass.getSimpleName}, no Zuora authentication")
    }

    Timing.record(metrics, action.getClass.getSimpleName) {
      WS.url(url.toString).post(action.xml)
    }.map { result =>
      metrics.putResponseCode(result.status, "POST")

      reader.read(result.body) match {
        case Left(error) =>
          if (error.fatal) {
            metrics.recordError
            Logger.error(s"Zuora action error ${action.getClass.getSimpleName} with status ${result.status} and body ${action.sanitized}")
            Logger.error(result.body)
          }

          throw error

        case Right(obj) => obj
      }
    }
  }

  def query[T <: ZuoraQuery](where: String)(implicit reader: ZuoraQueryReader[T]): Future[Seq[T]] = {
    request(Query(formatQuery(reader, where))).map { case QueryResult(results) => reader.read(results) }
  }

  def queryOne[T <: ZuoraQuery](where: String)(implicit reader: ZuoraQueryReader[T]): Future[T] = {
    query(where)(reader).map { results =>
      if (results.length != 1) {
        throw new ZuoraServiceError(s"Query '${reader.getClass.getSimpleName} $where' returned ${results.length} results, expected one")
      }

      results(0)
    }
  }
}
