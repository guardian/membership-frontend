package services

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.libs.json.Reads
import play.api.libs.ws.{Response, WS}

import model.Stripe._
import model.StripeDeserializer._
import configuration.Config

trait StripeService {
  def post[A <: StripeObject](endpoint: String, data: Map[String, Seq[String]])(implicit reads: Reads[A]): Future[A]
  def get[A <: StripeObject](endpoint: String)(implicit reads: Reads[A]): Future[A]

  private def extract[A <: StripeObject](response: Response)(implicit reads: Reads[A]): A = {
    response.json.asOpt[A].getOrElse {
      throw (response.json \ "error").asOpt[Error].getOrElse(Error("internal", "Unable to extract object"))
    }
  }

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
    def create(customerId: String, planId: String): Future[Subscription] = {
      post[Subscription](s"customers/$customerId/subscriptions", Map("plan" -> Seq(planId)))
    }
  }
}

object StripeService extends StripeService {
  val apiURL = Config.stripeApiURL
  val apiAuthHeader = ("Authorization", s"Bearer ${Config.stripeApiSecret}")

  def get[A <: StripeObject](endpoint: String)(implicit reads: Reads[A]): Future[A] =
    WS.url(s"$apiURL/$endpoint").withHeaders(apiAuthHeader).get().map(extract[A])

  def post[A <: StripeObject](endpoint: String, data: Map[String, Seq[String]])(implicit reads: Reads[A]): Future[A] =
    WS.url(s"$apiURL/$endpoint").withHeaders(apiAuthHeader).post(data).map(extract[A])
}
