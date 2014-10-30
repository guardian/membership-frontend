package utils

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.Logger
import play.api.libs.ws.{WSResponse, WSRequestHolder, WS}
import play.api.libs.json.Reads
import play.api.Play.current

import com.gu.monitoring.StatusMetrics

case class HttpError(http: Http[_, _ <: Throwable], response: WSResponse) extends Throwable {
  override def getMessage: String = s"${http.getClass.getSimpleName} - ${response.status}: ${response.body}"
}

trait Http[T, Error <: Throwable] {
  val apiUrl: String
  val statusMetrics: StatusMetrics

  def authenticateRequest(req: WSRequestHolder): WSRequestHolder

  private def request[A <: T](req: WSRequestHolder)(implicit reads: Reads[A], error: Reads[Error]): Future[A] = {
    authenticateRequest(req).execute().map { response =>
      statusMetrics.putResponseCode(response.status, req.method)

      response.json.asOpt[A].getOrElse {
        Logger.error(s"${getClass.getSimpleName} request failed with status code ${response.status}")
        Logger.error(req.body.toString)
        Logger.error(response.body)

        throw response.json.asOpt[Error].getOrElse(HttpError(this, response))
      }
    }.recover { case e =>
      Logger.error(s"${getClass.getSimpleName} request failed with exception ${e.getMessage}")
      throw e
    }
  }

  def get[A <: T](endpoint: String, params: (String, String)*)(implicit reads: Reads[A], error: Reads[Error]): Future[A] =
    request(WS.url(s"$apiUrl/$endpoint").withQueryString(params: _*).withMethod("GET"))

  def post[A <: T](endpoint: String, data: Map[String, Seq[String]])(implicit reads: Reads[A], error: Reads[Error]): Future[A] =
    request(WS.url(s"$apiUrl/$endpoint").withBody(data).withMethod("POST"))
}
