package com.gu.zuora.rest

import com.gu.monitoring.{NoOpZuoraMetrics, ZuoraMetrics}
import com.gu.zuora.ZuoraRestConfig
import okhttp3.{Response => OKHttpResponse, _}
import play.api.libs.json.{JsValue, Json, Reads, Writes}
import scalaz.syntax.std.either._
import scala.language.higherKinds
import scalaz.syntax.monad._
import scalaz.syntax.functor.ToFunctorOps
import scalaz.{Functor, \/}
import scala.util.{Success, Try}

/**
  * This is the smallest client required to talk to Zuora over REST
  * It just authenticates calls, adds the right URL and lets you send JSON via a particular request method
  */
case class SimpleClient[M[_] : Functor](
  config: ZuoraRestConfig,
  run: Request => M[OKHttpResponse],
  metrics: ZuoraMetrics = NoOpZuoraMetrics
) {
  def authenticated(url: String): Request.Builder = {
    metrics.countRequest() // to count total number of request hitting Zuora

    new Request.Builder()
      .addHeader("apiSecretAccessKey", config.password)
      .addHeader("apiAccessKeyId", config.username)
      .url(s"${config.url}/$url")
  }

  def isSuccess(statusCode: Int) = statusCode >= 200 && statusCode < 300

  def parseJson[B](in: OKHttpResponse): \/[String, JsValue] = {
    if (isSuccess(in.code)) {
      (for {
        body <- Try(in.body().string)
        json <- Try(Json.parse(body))
      } yield json) match {
        case Success(v) => \/.r[String](v)
        case scala.util.Failure(e) => \/.l[JsValue](e.toString)
      }
    } else {
      val bodyStr = Try(in.body.string).toOption
      \/.l[JsValue](s"response with status ${in.code}, body:$bodyStr")
    }
  }

  def parseResponse[B](in: OKHttpResponse)(implicit r: Reads[B]): String \/ B = {
    parseJson(in).flatMap { jsValue =>
      r.reads(jsValue).asEither.disjunction.leftMap(error => s"json was well formed but not matching the reader: $error, json was << ${Json.prettyPrint(jsValue)} >>")
    }
  }

  def body[A](in: A)(implicit w: Writes[A]): RequestBody = jsonBody(w.writes(in))

  def jsonBody(in: JsValue): RequestBody = RequestBody.create(MediaType.parse("application/json"), in.toString())

  def get[B](url: String)(implicit r: Reads[B]): M[String \/ B] =
    run(authenticated(url).get.build).map(parseResponse(_)(r))

  def getJson(url: String): M[String \/ JsValue] =
    run(authenticated(url).get.build).map(parseJson(_))

  def put[A, B](url: String, in: A)(implicit r: Reads[B], w: Writes[A]): M[String \/ B] =
    run(authenticated(url).put(body(in)).build).map(parseResponse(_)(r))

  def putJson [B](url: String, in: JsValue)(implicit r: Reads[B]): M[String \/ B] =
    run(authenticated(url).put(body(in)).build).map(parseResponse(_)(r))

  def post[A, B](url: String, in: A)(implicit r: Reads[B], w: Writes[A]): M[String \/ B] =
    run(authenticated(url).post(body(in)).build).map(parseResponse(_)(r))

  def postJson[B](url: String, in: JsValue)(implicit r: Reads[B]): M[String \/ B] =
    run(authenticated(url).post(body(in)).build).map(parseResponse(_)(r))
}
