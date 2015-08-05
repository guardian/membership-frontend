package services.zuora

import com.gu.lib.okhttpscala._
import com.gu.membership.util.Timing
import com.gu.monitoring.{AuthenticationMetrics, StatusMetrics}
import com.squareup.okhttp.OkHttpClient
import com.squareup.okhttp.Request.Builder
import model.Zuora.Feature
import monitoring.TouchpointBackendMetrics
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future
import play.api.libs.json._
import play.api.libs.json.Reads._
import play.api.libs.functional.syntax._

import scala.util.{Failure, Success, Try}

class ZuoraRestService(restUrl: String,
                       username: String,
                       password: String,
                       environment: String) {

  import ZuoraRestResponseReaders._

  private val client = new OkHttpClient

  val metrics = new TouchpointBackendMetrics with StatusMetrics with AuthenticationMetrics {
    val backendEnv = environment
    val service = "ZuoraRestClient"

    def recordError() {
      put("error-count", 1)
    }
  }

  private def get(uri: String) = client.execute(new Builder()
    .addHeader("apiAccessKeyId", username)
    .addHeader("apiSecretAccessKey", password)
    .addHeader("Accept", "application/json")
    .url(s"$restUrl/$uri")
    .get().build())

  /**
   * @see https://knowledgecenter.zuora.com/BC_Developers/REST_API/B_REST_API_reference/Accounts/2_Get_account_basics
   * @param accountKey Account number or account ID
   * @return
   */
  def productFeaturesByAccount(accountKey: String): Future[Seq[Feature]] =
    Timing.record(metrics, "productFeaturesByAccount") {
      get("/accounts/").map { response =>
        metrics.putResponseCode(response.code, "GET")
        Try(extractFeatures(response.body().string())) match {
          case Success(x) => x
          case Failure(e) =>
            metrics.recordError()
            throw e
        }
      }
    }

}

object ZuoraRestResponseReaders {

  implicit val featureReads: Reads[Feature] = (
    (JsPath \ "id").read[String] and
      (JsPath \ "featureCode").read[String]
    )(Feature.apply _)

  def extractFeatures(responseBody: String): Seq[Feature] =
  {
    println(responseBody)
    (Json.parse(responseBody) \\ "subscriptionProductFeatures")
      .flatMap(_.as[Seq[Feature]])
  }

}
