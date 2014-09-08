package services

import model.Stripe._
import model.StripeDeserializer._
import monitoring.StripeMetrics
import play.api.Logger
import play.api.Play.current
import play.api.libs.json.Reads
import play.api.libs.ws.{WS, WSRequestHolder, WSResponse}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

case class StripeApiConfig(url: String, secretKey: String, publicKey: String)

class StripeService(apiConfig: StripeApiConfig) {

  val apiAuthHeader = ("Authorization", s"Bearer ${apiConfig.secretKey}")

  def apiRequest(endpoint: String): WSRequestHolder =
    WS.url(s"${apiConfig.url}/$endpoint").withHeaders(apiAuthHeader)

  def get[A <: StripeObject](endpoint: String)(implicit reads: Reads[A]): Future[A] =
    apiRequest(endpoint).get().map { response =>
      recordAndLogResponse(response.status, "GET", endpoint)
      extract[A](response)
    }

  def post[A <: StripeObject](endpoint: String, data: Map[String, Seq[String]])(implicit reads: Reads[A]): Future[A] =
    apiRequest(endpoint).post(data).map { response =>
      recordAndLogResponse(response.status, "POST", endpoint)
      extract[A](response)
    }

  def delete[A <: StripeObject](endpoint: String)(implicit reads: Reads[A]): Future[A] =
    apiRequest(endpoint).delete().map { response =>
      recordAndLogResponse(response.status, "DELETE", endpoint)
      extract[A](response)
    }

  private def extract[A <: StripeObject](response: WSResponse)(implicit reads: Reads[A]): A = {
    response.json.asOpt[A].getOrElse {
      throw (response.json \ "error").asOpt[Error].getOrElse(Error("internal", "Unable to extract object", None, None))
    }
  }

  private def recordAndLogResponse(status: Int, responseMethod: String, endpoint: String) {
    Logger.info(s"$responseMethod response ${status} for endpoint ${endpoint}")
    StripeMetrics.putResponseCode(status, responseMethod)
  }

  object Customer {
    def create(identityId: String, card: String): Future[Customer] =
      post[Customer]("customers", Map("description" -> Seq(s"IdentityID - $identityId"), "card" -> Seq(card)))

    def read(customerId: String): Future[Customer] =
      get[Customer](s"customers/$customerId")

    def updateCard(customerId: String, card: String): Future[Customer] =
      post[Customer](s"customers/$customerId", Map("card" -> Seq(card)))
  }
}
