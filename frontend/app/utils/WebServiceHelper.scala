package utils

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.Logger
import play.api.libs.ws.{WSResponse, WSRequestHolder, WS}
import play.api.libs.json.Reads
import play.api.Play.current

import com.gu.monitoring.StatusMetrics

case class WebServiceHelperError(http: WebServiceHelper[_, _ <: Throwable], response: WSResponse) extends Throwable {
  override def getMessage: String = s"${http.getClass.getSimpleName} - ${response.status}: ${response.body}"
}

/**
 * A wrapper for a JSON web service. It automatically converts the response to a solid type, or
 * handles and logs the error.
 *
 * @tparam T The base type of all the objects that this WebServiceHelper can return
 * @tparam Error The type that will attempt to be extracted if extracting the expected object fails.
 *               This is useful when a web service has a standard error format
 */
trait WebServiceHelper[T, Error <: Throwable] {
  val wsUrl: String
  val wsMetrics: StatusMetrics

  /**
   * Manipulate the request before it is executed. Generally used to add any authentication settings
   * the web services requires (e.g. add an Authentication header)
   *
   * @param req The request
   * @return The modified request
   */
  def wsPreExecute(req: WSRequestHolder): WSRequestHolder

  /**
   * Send a request to the web service and attempt to convert the response to an A
   *
   * @param req The request to send
   * @param reads A Reader to convert JSON to A
   * @param error A Reader to convert JSON to Error
   * @tparam A The type of the object that is expected to be returned from the request
   * @return
   */
  private def request[A <: T](req: WSRequestHolder)(implicit reads: Reads[A], error: Reads[Error]): Future[A] = {
    wsPreExecute(req).execute().map { response =>
      wsMetrics.putResponseCode(response.status, req.method)

      response.json.asOpt[A].getOrElse {
        Logger.error(s"${getClass.getSimpleName} request failed with status code ${response.status}")
        Logger.error(req.body.toString)
        Logger.error(response.body)

        throw response.json.asOpt[Error].getOrElse(WebServiceHelperError(this, response))
      }
    }.recover { case e =>
      Logger.error(s"${getClass.getSimpleName} request failed with exception ${e.getMessage}")
      throw e
    }
  }

  def get[A <: T](endpoint: String, params: (String, String)*)(implicit reads: Reads[A], error: Reads[Error]): Future[A] =
    request(WS.url(s"$wsUrl/$endpoint").withQueryString(params: _*).withMethod("GET"))

  def post[A <: T](endpoint: String, data: Map[String, Seq[String]])(implicit reads: Reads[A], error: Reads[Error]): Future[A] =
    request(WS.url(s"$wsUrl/$endpoint").withBody(data).withMethod("POST"))
}
