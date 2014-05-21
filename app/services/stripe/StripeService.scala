package services.stripe

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.libs.json.{ Json, Reads }
import play.api.libs.ws.{ Response, WS }

import model.stripe._

trait StripeService {
  protected val apiURL: String
  protected val apiSecret: String

  implicit val readsError = Json.reads[Error]
  implicit val readsCard = Json.reads[Card]
  implicit val readsCharge = Json.reads[Charge]
  implicit val readsCustomer = Json.reads[Customer]
  implicit val readsSubscription = Json.reads[Subscription]

  private def request(endpoint: String) =
    WS.url(s"$apiURL/$endpoint").withHeaders(("Authorization", s"Bearer $apiSecret"))

  private def extract[A <: StripeObject](response: Response)(implicit reads: Reads[A]): A = {
    response.json.asOpt[A].getOrElse {
      throw (response.json \ "error").asOpt[Error].getOrElse(Error("internal", "Unable to extract object"))
    }
  }

  def post[A <: StripeObject](endpoint: String, data: Map[String, Seq[String]])(implicit reads: Reads[A]): Future[A] =
    request(endpoint).post(data).map(extract[A])

  def get[A <: StripeObject](endpoint: String)(implicit reads: Reads[A]): Future[A] =
    request(endpoint).get().map(extract[A])
}
