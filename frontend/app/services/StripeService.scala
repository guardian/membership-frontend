package services

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.libs.json.{ Json, Reads }
import play.api.libs.ws.{ Response, WS }

import model.Stripe._
import configuration.Config

trait StripeService {
  protected val apiURL: String
  protected val apiSecret: String

  implicit val readsError = Json.reads[Error]
  implicit val readsCard = Json.reads[Card]
  implicit val readsCharge = Json.reads[Charge]
  implicit val readsSubscription = Json.reads[Subscription]
  implicit val readsSubscriptionList = Json.reads[SubscriptionList]
  implicit val readsCustomer = Json.reads[Customer]

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

object StripeService extends StripeService {
  protected val apiURL = Config.stripeApiURL
  protected val apiSecret = Config.stripeApiSecret

  object Charge {
    def create(amount: Int, currency: String, card: String, description: String): Future[Charge] = {
      post[Charge]("charges", Map(
        "amount" -> Seq(amount.toString),
        "currency" -> Seq(currency),
        "card" -> Seq(card),
        "description" -> Seq(description)
      ))
    }
  }

  object Customer {
    def create(card: String): Future[Customer] =
      post[Customer]("customers", Map("card" -> Seq(card)))

    def read(customerId: String): Future[Customer] =
      get[Customer](s"customers/$customerId")
  }

  object Subscription {
    def create(customerId: String, planId: String): Future[Subscription] =
      post[Subscription](s"customers/$customerId", Map("plan" -> Seq(planId)))
  }
}
