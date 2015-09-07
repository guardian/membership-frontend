package services.zuora

import com.github.nscala_time.time.JodaImplicits._
import com.gu.membership.util.{FutureSupplier, Timing}
import com.gu.membership.zuora.ZuoraApiConfig
import com.gu.monitoring.{AuthenticationMetrics, StatusMetrics}
import com.typesafe.scalalogging.LazyLogging
import model.FeatureChoice
import model.Zuora._
import model.ZuoraDeserializer._
import model.ZuoraReaders._
import monitoring.TouchpointBackendMetrics
import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, DateTimeZone, ReadableDuration}
import play.api.Logger
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.ws.WS
import utils.ScheduledTask

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

class ZuoraSoapService(val apiConfig: ZuoraApiConfig) extends LazyLogging {

  import ZuoraServiceHelpers._

  val metrics = new TouchpointBackendMetrics with StatusMetrics with AuthenticationMetrics {
    val backendEnv = apiConfig.envName

    val service = "Zuora"

    def recordError() {
      put("error-count", 1)
    }
  }

  private val authSupplier =
    new FutureSupplier[Authentication](request(Login(apiConfig)))

  val featuresSupplier =
    new FutureSupplier[Seq[Feature]](getFeatures)

  val lastPingTime = ScheduledTask[Option[DateTime]]("ZuoraPing", None, 0.seconds, 30.seconds)(
    authenticatedRequest(Query("SELECT Id FROM Product")).map { _ => Some(new DateTime) }
  )

  def lastPingTimeWithin(duration: ReadableDuration): Boolean =
    lastPingTime.get().exists { t => t > DateTime.now - duration }

  private val scheduler = Akka.system.scheduler

  List(
    (30.minutes, authSupplier),
    (30.minutes, featuresSupplier)
  ) foreach { case (duration, supplier) =>
    scheduler.schedule(duration, duration) {supplier.refresh()}
  }
  lastPingTime.start()

  def getAuth = authSupplier.get()

  def authenticatedRequest[T <: ZuoraResult](action: => ZuoraAction[T])(implicit reader: ZuoraReader[T]): Future[T] =
    getAuth.flatMap { auth => request(action, Some(auth)) }

  def query[T <: ZuoraQuery](where: String)(implicit reader: ZuoraQueryReader[T]): Future[Seq[T]] = {
    authenticatedRequest(Query(formatQuery(reader, where))).map { case QueryResult(results) => reader.read(results) }
  }

  def queryOne[T <: ZuoraQuery](where: String)(implicit reader: ZuoraQueryReader[T]): Future[T] = {
    query(where)(reader).map { results =>
      if (results.length != 1) {
        throw new ZuoraServiceError(s"Query '${reader.getClass.getSimpleName} $where' returned ${results.length} results, expected one")
      }
      results.head
    }
  }

  private def request[T <: ZuoraResult](action: ZuoraAction[T], authOpt: Option[Authentication] = None)
                                       (implicit reader: ZuoraReader[T]): Future[T] = {

    val url = apiConfig.url.toString()

    Timing.record(metrics, action.getClass.getSimpleName) {
      WS.url(url).post(action.xml(authOpt).toString())
    }.map { result =>
      metrics.putResponseCode(result.status, "POST")
      reader.read(result.body) match {
        case Left(error) =>
          if (error.fatal) {
            metrics.recordError()
            Logger.error(s"Zuora action error ${action.getClass.getSimpleName} with status ${result.status} and body ${action.sanitized}")
            Logger.error(result.body)
          }
          throw error

        case Right(obj) => obj
      }
    }
  }

  /*  private def getBillingInfoForProducRatePlans(productRatePlans: Seq[ProductRatePlan]) = {
        val activeProductRatePlans: Seq[ProductRatePlan] = productRatePlans.filter(_.isActive)
  }*/


  private def getFeatures: Future[Seq[Feature]] = {
    val featuresF = query[Feature]("Status = 'Active'")
    featuresF.foreach { features =>
      val diff = FeatureChoice.codes &~ features.map(_.code).toSet
      lazy val msg =
        s"""
           |Zuora ${apiConfig.envName} is missing the following product features:
                                        |${diff.mkString(", ")}. Please update configuration ASAP!"""
          .stripMargin

      if (diff.nonEmpty) logger.error(msg)
    }

    featuresF
  }
}
