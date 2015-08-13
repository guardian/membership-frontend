package services.zuora

import com.gu.lib.okhttpscala._
import com.gu.membership.util.Timing
import com.gu.membership.zuora.ZuoraApiConfig
import com.gu.monitoring.{AuthenticationMetrics, StatusMetrics}
import com.squareup.okhttp.{Response, OkHttpClient}
import com.squareup.okhttp.Request.Builder
import model.Zuora.{Subscription, Feature}
import monitoring.TouchpointBackendMetrics
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

import scala.util.{Failure, Success, Try}

object ZuoraRestService {
  sealed trait RestResponse[+T] {
    def isSuccess: Boolean
  }
  case class RestSuccess[T](value: T) extends RestResponse[T] {
    override def isSuccess = true
  }

  case class RestError(processId: String, reasons: Seq[ErrorMsg]) extends RestResponse[Nothing] {
    override def isSuccess = false
  }

  case class ErrorMsg(code: String, msg: String)
}

class ZuoraRestService(config: ZuoraApiConfig) {

  import ZuoraRestResponseReaders._

  private val client = new OkHttpClient

  val metrics = new TouchpointBackendMetrics with StatusMetrics with AuthenticationMetrics {
    val backendEnv = config.envName
    val service = "ZuoraRestClient"

    def recordError() {
      put("error-count", 1)
    }
  }

  def subscriptionsByAccount(accountKey: String): Future[Seq[Subscription]] =
    Timing.record(metrics, "subscriptionsByAccount") {
      get(s"subscriptions/accounts/$accountKey").map { response =>
        metrics.putResponseCode(response.code, "GET")
        None
        ???
      }
    }

  /**
   * @see https://knowledgecenter.zuora.com/BC_Developers/REST_API/B_REST_API_reference/Subscriptions/4_Get_subscriptions_by_account
   * @param accountKey Account number or account ID
   * @return
   */
  def productFeaturesByAccount(accountKey: String): Future[Seq[Feature]] =
    Timing.record(metrics, "productFeaturesByAccount") {
      get(s"subscriptions/accounts/$accountKey").map { response =>
        metrics.putResponseCode(response.code, "GET")
        Try(extractFeatures(response.body().string())) match {
          case Success(x) => x
          case Failure(e) =>
            metrics.recordError()
            throw e
        }
      }
    }


  private def get(uri: String): Future[Response] = client.execute(new Builder()
    .addHeader("apiAccessKeyId", config.username)
    .addHeader("apiSecretAccessKey", config.password)
    .addHeader("Accept", "application/json")
    .url(s"${config.url}/$uri")
    .get().build())
}

object ZuoraRestResponseReaders {
  import ZuoraRestService._

  implicit val errorMsgReads: Reads[ErrorMsg] = Json.reads[ErrorMsg]

  def parseResponse[T](body: String)(implicit ev: Reads[T]): RestResponse[T] = {
    val json = Json.parse(body)
    val isSuccess = (json \ "success").as[Boolean]
    if (isSuccess) RestSuccess(json.as[T]) else json.as[RestError]
  }

  implicit val errorReads: Reads[RestError] =
    ((JsPath \ "processId").read[String] and
      (JsPath \ "errors").read[Seq[ErrorMsg]])(RestError.apply _)

  implicit val featureReads: Reads[Feature] = (
    (JsPath \ "id").read[String] and
      (JsPath \ "featureCode").read[String]
    )(Feature.apply _)

  implicit val subscriptionReads: Reads[Subscription] = Json.reads[Subscription]

  def extractFeatures(responseBody: String): Seq[Feature] =
    (Json.parse(responseBody) \\ "subscriptionProductFeatures")
      .flatMap(_.as[Seq[Feature]])

}
