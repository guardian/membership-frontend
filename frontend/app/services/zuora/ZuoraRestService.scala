package services.zuora

import com.github.nscala_time.time.OrderingImplicits.DateTimeOrdering
import com.gu.lib.okhttpscala._
import com.gu.membership.util.{FutureSupplier, Timing}
import com.gu.membership.zuora.ZuoraApiConfig
import com.gu.monitoring.{AuthenticationMetrics, StatusMetrics}
import com.squareup.okhttp.Request.Builder
import com.squareup.okhttp.{OkHttpClient, Response}
import com.typesafe.scalalogging.LazyLogging
import monitoring.TouchpointBackendMetrics
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import services.SubscriptionServiceError
import services.zuora.Rest.{ProductRatePlanCharge, ProductCatalog, ProductRatePlan}

import scala.concurrent.Future

class ZuoraRestService(config: ZuoraApiConfig) extends LazyLogging {

  import ZuoraRestResponseReaders._

  private val client = new OkHttpClient

  val metrics = new TouchpointBackendMetrics with StatusMetrics with AuthenticationMetrics {
    val backendEnv = config.envName
    val service = "ZuoraRestClient"

    def recordError() {
      put("error-count", 1)
    }
  }

  val productCatalogSupplier = new FutureSupplier[ProductCatalog](productCatalog)

  def subscriptionsByAccounts(accountKeys: Set[String]): Future[Seq[Rest.Subscription]] =
    Future.reduce(accountKeys.map(subscriptionsByAccount))(_ ++ _)

  /**
   * Fetch all subscriptions associated to a given account id
   *
   * @see https://knowledgecenter.zuora.com/BC_Developers/REST_API/B_REST_API_reference/Subscriptions/4_Get_subscriptions_by_account
   * @param accountKey Account number or account ID
   */
  def subscriptionsByAccount(accountKey: String): Future[List[Rest.Subscription]] =
    Timing.record(metrics, "subscriptionsByAccount") {
      get(s"subscriptions/accounts/$accountKey").map { response =>
        metrics.putResponseCode(response.code, "GET")
        parseResponse[List[Rest.Subscription]](response) match {
          case Rest.Success(subscriptions) => subscriptions
          case Rest.Failure(_, errors) => {
            logger.error(s"Cannot find any subscriptions for account id: $accountKey (errors: $errors)")
            Nil
          }
        }
      }
    }

  def productCatalog: Future[ProductCatalog] = Timing.record(metrics, "catalog") {
    get("catalog/products").map { response =>
      metrics.putResponseCode(response.code, "GET")
      parseResponse[ProductCatalog](response).get
    }
  }

  def lastSubscriptionWithProductOfTypeOpt(prodType: String, accountIds: Set[String]): Future[Option[Rest.Subscription]] = for {
    catalog <- productCatalogSupplier.get()
    subscriptions <- subscriptionsByAccounts(accountIds)
  } yield subscriptions.filter(
      _.hasWhitelistedProduct(catalog.productIdsOfType(prodType))
    ).sortBy(_.termStartDate).lastOption

  def lastSubscriptionWithProductOfType(prodType: String, accountIds: Set[String]): Future[Rest.Subscription] =
    lastSubscriptionWithProductOfTypeOpt(prodType, accountIds).map(_.getOrElse {
      throw new SubscriptionServiceError(s"Cannot find a membership subscription for account ids $accountIds")
    })

  private def get(uri: String): Future[Response] = client.execute(new Builder()
    .addHeader("apiAccessKeyId", config.username)
    .addHeader("apiSecretAccessKey", config.password)
    .addHeader("Accept", "application/json")
    .url(s"${config.url}/$uri")
    .get().build())

}

object ZuoraRestResponseReaders {
  def parseResponse[T: Reads](resp: Response): Rest.Response[T] =
    parseResponse[T](Json.parse(resp.body().string()))

  def parseResponse[T: Reads](json: JsValue): Rest.Response[T] = {
    val isSuccess = (json \ "success").as[Boolean]
    if (isSuccess) Rest.Success(json.as[T]) else json.as[Rest.Failure]
  }

  def productFeatures(subscription: Rest.Subscription): Seq[Rest.Feature] =
    subscription.ratePlans.headOption.map(_.subscriptionProductFeatures).getOrElse(Nil)

  implicit val subscriptionStatus = new Reads[Rest.SubscriptionStatus] {

    import Rest._

    override def reads(v: JsValue): JsResult[SubscriptionStatus] = v match {
      case JsString("Draft") => JsSuccess(Draft)
      case JsString("PendingActivation") => JsSuccess(PendingActivation)
      case JsString("PendingAcceptance") => JsSuccess(PendingAcceptance)
      case JsString("Active") => JsSuccess(Active)
      case JsString("Cancelled") => JsSuccess(Cancelled)
      case JsString("Expired") => JsSuccess(Expired)
      case other => JsError(s"Cannot parse a Rest.SubscriptionStatus from object $other")
    }
  }

  implicit val errorMsgReads: Reads[Rest.Error] = Json.reads[Rest.Error]
  implicit val failureReads: Reads[Rest.Failure] = (
    (JsPath \ "processId").read[String] and
      (JsPath \ "reasons").read[List[Rest.Error]]
    )(Rest.Failure.apply _)

  implicit val productRatePlanChargeReads: Reads[ProductRatePlanCharge] = Json.reads[Rest.ProductRatePlanCharge]
  implicit val productRatePlanReads: Reads[ProductRatePlan] = Json.reads[Rest.ProductRatePlan]
  implicit val productReads: Reads[Rest.Product] = Json.reads[Rest.Product]
  implicit val catalogReads: Reads[Rest.ProductCatalog] = Json.reads[Rest.ProductCatalog]
  implicit val featureReads: Reads[Rest.Feature] = Json.reads[Rest.Feature]

  implicit val subscriptionListReads: Reads[List[Rest.Subscription]] = for {
    subscriptionArray <- (JsPath \ "subscriptions").read[JsArray]
    subscriptions = subscriptionArray.value.map(_.as[Rest.Subscription]).toList
  } yield subscriptions

  implicit val ratePlanChargeReader: Reads[Rest.RatePlanCharge] = Json.reads[Rest.RatePlanCharge]
  implicit val ratePlanReader: Reads[Rest.RatePlan] = Json.reads[Rest.RatePlan]
  implicit val subscriptionReads: Reads[Rest.Subscription] = Json.reads[Rest.Subscription]
}
