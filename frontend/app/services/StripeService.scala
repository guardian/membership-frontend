package services

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.Play.current
import play.api.libs.json.Reads
import play.api.libs.ws.{WSResponse, WS}
import play.api.Logger

import com.gu.membership.salesforce.{Member, Tier}

import model.Stripe._
import model.StripeDeserializer._
import configuration.Config

trait StripeService {
  def get[A <: StripeObject](endpoint: String)(implicit reads: Reads[A]): Future[A]
  def post[A <: StripeObject](endpoint: String, data: Map[String, Seq[String]])(implicit reads: Reads[A]): Future[A]
  def delete[A <: StripeObject](endpoint: String)(implicit reads: Reads[A]): Future[A]

  private def extract[A <: StripeObject](response: WSResponse)(implicit reads: Reads[A]): A = {
    response.json.asOpt[A].getOrElse {
      throw (response.json \ "error").asOpt[Error].getOrElse(Error("internal", "Unable to extract object", None, None))
    }
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

object StripeService extends StripeService {
  val apiURL = Config.stripeApiURL
  val apiAuthHeader = ("Authorization", s"Bearer ${Config.stripeApiKeySecret}")

  def get[A <: StripeObject](endpoint: String)(implicit reads: Reads[A]): Future[A] =
    WS.url(s"$apiURL/$endpoint").withHeaders(apiAuthHeader).get().map(extract[A])

  def post[A <: StripeObject](endpoint: String, data: Map[String, Seq[String]])(implicit reads: Reads[A]): Future[A] =
    WS.url(s"$apiURL/$endpoint").withHeaders(apiAuthHeader).post(data).map(extract[A])

  def delete[A <: StripeObject](endpoint: String)(implicit reads: Reads[A]): Future[A] =
    WS.url(s"$apiURL/$endpoint").withHeaders(apiAuthHeader).delete().map(extract[A])
}
