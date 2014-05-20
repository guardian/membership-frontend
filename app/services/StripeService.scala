package services

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.libs.ws.{ Response, WS }
import play.api.libs.json.{ Reads, Json }

import com.typesafe.config.ConfigFactory

import model.Stripe._

trait StripeService {
  protected val apiURL: String
  protected val apiSecret: String

  implicit val readsError = Json.reads[Error]
  implicit val readsCard = Json.reads[Card]
  implicit val readsCharge = Json.reads[Charge]

  private def request(endpoint: String) =
    WS.url(s"$apiURL/$endpoint").withHeaders(("Authorization", s"Bearer $apiSecret"))

  private def extract[A <: StripeObject](response: Response)(implicit reads: Reads[A]) = {
    response.json.asOpt[A].toRight {
      (response.json \ "error").asOpt[Error].getOrElse {
        Error("internal", "Unable to extract object")
      }
    }
  }

  def post[A <: StripeObject](endpoint: String, data: Map[String, Seq[String]])(implicit reads: Reads[A]): Future[Either[Error, A]] =
    request(endpoint).post(data).map(extract[A])

  def get[A <: StripeObject](endpoint: String)(implicit reads: Reads[A]): Future[Either[Error, A]] =
    request(endpoint).get().map(extract[A])
}

object StripeService extends StripeService {
  private val config = ConfigFactory.load()

  protected val apiURL = config.getString("stripe.api.url")
  protected val apiSecret = config.getString("stripe.api.secret")

  object Charge {
    def create(amount: Int, currency: String, card: String, description: String): Future[Either[Error, Charge]] = {
      post[Charge]("charges", Map(
        "amount" -> Seq(amount.toString),
        "currency" -> Seq(currency),
        "card" -> Seq(card),
        "description" -> Seq(description)
      ))
    }
  }
}

