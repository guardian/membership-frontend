package com.gu.zuora.soap

import akka.actor.ActorSystem
import com.github.nscala_time.time.JodaImplicits._
import okhttp3.Request.Builder
import okhttp3._
import java.util.concurrent.atomic.AtomicReference
import com.gu.memsub.util.ScheduledTask
import com.gu.memsub.util.FutureRetry._
import com.gu.zuora.ZuoraSoapConfig
import com.gu.zuora.soap.Readers._
import com.gu.zuora.soap.actions.{Action, Actions}
import com.gu.zuora.soap.models.{Identifiable, Query, Result}
import com.gu.zuora.soap.models.Results.{Authentication, QueryResult}
import com.gu.zuora.soap.readers.Reader
import com.gu.okhttp.RequestRunners._
import com.gu.monitoring.{NoOpZuoraMetrics, SafeLogger, ZuoraMetrics}
import com.gu.monitoring.SafeLogger._
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.{DateTime, ReadableDuration}

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.reflect._
import scala.util.{Failure, Success}

class Client(
  apiConfig: ZuoraSoapConfig,
  httpClient: FutureHttpClient,
  extendedHttpClient:
  FutureHttpClient,
  metrics: ZuoraMetrics = NoOpZuoraMetrics
)(implicit actorSystem: ActorSystem, ec: ExecutionContext) extends LazyLogging {

  import Client._

  val clientMediaType = MediaType.parse("text/plain; charset=utf-8")

  private val periodicAuth = new AtomicReference[Authentication](null)
  actorSystem.scheduler.schedule(0.seconds, 15.minutes)(authentication())

  private def authentication(): Unit =
    retry(request(Actions.Login(apiConfig), None, authenticationReader))(ec, actorSystem.scheduler)
      .onComplete {
        case Success(auth) =>
          periodicAuth.set(auth)
          logger.info(s"Successfully authenticated Zuora SOAP client in ${apiConfig.envName}")

        case Failure(ex) =>
          logger.error(s"Failed Zuora SOAP client authentication in ${apiConfig.envName}", ex)
      }

  private def request[T <: models.Result](action: Action[T],
                                          authentication: Option[Authentication],
                                          reader: Reader[T],
                                          client: FutureHttpClient = httpClient): Future[T] = {
    metrics.countRequest()
    val request = new Builder().url(apiConfig.url.toString())
      .post(RequestBody.create(clientMediaType, action.xml(authentication).toString()))
      .build()
    if (action.enableLogging)
      SafeLogger.info(s"Zuora SOAP call in environment ${apiConfig.envName}. Request info:\n${action.prettyLogInfo}. Is authentication defined: ${authentication.isDefined}")
    client(request)
      .map { result =>
        val responseBody = result.body().string()
        reader.read(responseBody) match {
          case Left(error) =>
            SafeLogger.error(scrub"Zuora action ${action.getClass.getSimpleName} resulted in error: CODE: ${result.code} RESPONSE BODY: $responseBody Is authentication defined: ${authentication.isDefined}")
            throw error

          case Right(obj) => obj
        }
      }

  }

  def isReady: Boolean = Option(periodicAuth.get()).isDefined

  def authenticatedRequest[T <: Result](action: => Action[T])(implicit reader: Reader[T]): Future[T] =
    Future.unit.flatMap(_ => request(action, Option(periodicAuth.get), reader))

  def query[T <: Query](where: String)(implicit reader: readers.Query[T]): Future[Seq[T]] =
    authenticatedRequest(Actions.Query(reader.format(where))).map { case QueryResult(results) => reader.read(results) }

  def query[T <: Query](where: ZuoraFilter)(implicit reader: readers.Query[T]): Future[Seq[T]] =
    query(where.toFilterString)(reader)

  def queryOne[T <: Query](where: String)(implicit reader: readers.Query[T]): Future[T] =
    query(where)(reader).map(_.headOption
      .getOrElse(throw new ZuoraQueryException(s"Query '${reader.getClass.getSimpleName} $where' returned 0 results, expected one")))

  def queryOne[T <: Query](where: ZuoraFilter)(implicit reader: readers.Query[T]): Future[T] =
    queryOne(where.toFilterString)(reader)

  def parent[P <: Query, C <: Query](child: C, foreignKey: C => String)(implicit reader: readers.Query[P]): Future[P] =
    queryOne[P](parentFilter(child, foreignKey))(reader)

  def children[P <: Query with Identifiable, C <: Query](parent: P)(implicit reader: readers.Query[C]): Future[Seq[C]] =
    query[C](childFilter(parent))(reader)

  def child[P <: Query with Identifiable, C <: Query](parent: P)(implicit reader: readers.Query[C]): Future[C] =
    queryOne[C](childFilter(parent))(reader)

  private val lastPingTimeTask =
    ScheduledTask[Option[DateTime]]("ZuoraPing", None, 0.seconds, 30.seconds) {
      val result = authenticatedRequest(Actions.Query("SELECT Id FROM Product", enableLog = false))
      result.onComplete {
        case Success(r) => SafeLogger.debug(s"${apiConfig.envName} ZuoraPing successfully executed query. There are ${r.results.size} products.")
        case Failure(e) => SafeLogger.error(scrub"Scheduled Task: ${apiConfig.envName} ZuoraPing failed to execute query: ${e.getMessage}")
      }
      result.map { _ => Some(new DateTime) }
    }
  lastPingTimeTask.start()

  def lastPingTimeWithin(duration: ReadableDuration): Boolean =
    lastPingTimeTask.get().exists { t => t > (DateTime.now - duration) }
}

object Client {
  implicit class ChildOps[C <: Query](child: C) {
    def parent[P <: Query](foreignKey: C => String)(implicit client: Client, reader: readers.Query[P]) =
      client.parent(child, foreignKey)(reader)
  }

  implicit class ParentOps(parent: Query with Identifiable) {
    def children[C <: Query](implicit client: Client, reader: readers.Query[C]) =
      client.children(parent)(reader)

    def child[C <: Query](implicit client: Client, reader: readers.Query[C], ct: ClassTag[C]) =
      client.child(parent)(reader)
  }

  def parentFilter[C <: Query, P <: Query](child: C, foreignKey: (C) => String): SimpleFilter =
    SimpleFilter("Id", foreignKey(child))

  def childFilter[C <: Query, P <: Query with Identifiable](parent: P): SimpleFilter =
    SimpleFilter(parent.objectName +  "Id", parent.id)
}
