package com.gu.okhttp

import java.util.concurrent.TimeUnit
import com.gu.lib.okhttpscala._
import com.gu.memsub.util.Timing
import com.gu.monitoring.StatusMetrics
import okhttp3.{OkHttpClient, Request, Response => OkResponse}
import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.higherKinds
import scalaz.{Kleisli, ReaderT}

/**
  * These are functions from an OkHttpRequest to an M[Response] which are passed into Clients (such as SimpleClient),
  * to determine how they process HTTP requests
  */
object RequestRunners {
  lazy val client = new OkHttpClient()
  type FutureHttpClient = Request => Future[OkResponse]

  /**
    * Standard no frills run this request and return a response asynchronously
    * A solid choice for the beginner SimpleClient user
    */
  def futureRunner(implicit ec: ExecutionContext): Request => Future[OkResponse] =
    client.execute

  /** Adjusts the standard client used in futureRunner to use a configurable read timeout setting, see:
    * https://github.com/square/okhttp/wiki/Recipes#timeouts
    */
  def configurableFutureRunner(timeout: Duration)(implicit ec: ExecutionContext): Request => Future[OkResponse] = {
    val seconds: Int = timeout.toSeconds.toInt
    client.newBuilder().readTimeout(seconds, TimeUnit.SECONDS).build().execute
  }

  /**
    * Logging runner with the default read timeout (10 seconds).
    */
  def loggingRunner(metrics: StatusMetrics)(implicit ec: ExecutionContext): Request => ReaderT[String, Future, OkResponse] =
  configurableLoggingRunner(10.seconds, metrics)

  /** This is a runner that times how long calls takes with cloudwatch and logs details of them.
    * It also allows the read timeout for the OkHttpClient to be configured.
    * It seems Kleisli is basically a name for a ReaderT, in that what we want to do is return a reader monad
    * that given a string for the request ID will run the request and log its details against that request ID
    */
  def configurableLoggingRunner(timeout: Duration, metrics: StatusMetrics)
                               (implicit ec: ExecutionContext): Request => ReaderT[String, Future, OkResponse] = r => Kleisli {
    requestId => val resp = Timing.record(metrics, requestId) {
      val seconds: Int = timeout.toSeconds.toInt
      client.newBuilder().readTimeout(seconds, TimeUnit.SECONDS).build().execute(r)
    }
      resp.foreach(r => metrics.putResponseCode(r.code, r.request.method))
      resp
  }

}
