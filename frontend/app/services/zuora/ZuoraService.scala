package services.zuora

import com.gu.membership.util.Timing
import model.TierPlan
import model.Zuora._
import model.ZuoraDeserializer._
import model.ZuoraReaders._
import monitoring.ZuoraMetrics
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import play.api.Logger
import play.api.libs.ws.WS
import utils.ScheduledTask

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.xml.PrettyPrinter
import play.api.Play.current

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

case class ZuoraApiConfig(url: String, username: String, password: String, tierRatePlanIds: Map[TierPlan, String])

class ZuoraService(val apiConfig: ZuoraApiConfig) extends ScheduledTask[Authentication] {
  import ZuoraServiceHelpers._

  val initialValue = Authentication("", "")
  val initialDelay = 0.seconds
  val interval = 30.minutes

  def refresh() = request(Login(apiConfig))

  implicit def authentication: Authentication = agent.get()

  def request[T <: ZuoraResult](action: ZuoraAction[T])(implicit reader: ZuoraReader[T]): Future[T] = {
    val url = if (action.authRequired) authentication.url else apiConfig.url

    if (action.authRequired && authentication.url.length == 0) {
      ZuoraMetrics.putAuthenticationError
      throw ZuoraServiceError(s"Can't build authenticated request for ${action.getClass.getSimpleName}, no Zuora authentication")
    }

    Timing.record(ZuoraMetrics, action.getClass.getSimpleName) {
      WS.url(url).post(action.xml)
    }.map { result =>
      ZuoraMetrics.putResponseCode(result.status, "POST")

      reader.read(result.xml) match {
        case Left(error) =>
          if (error.fatal) {
            ZuoraMetrics.recordError
            Logger.error(s"Zuora action error ${action.getClass.getSimpleName} with status ${result.status} and body ${action.xml}")
            Logger.error(new PrettyPrinter(70, 2).format(result.xml))
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
