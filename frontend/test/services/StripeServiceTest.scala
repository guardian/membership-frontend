package services

import scala.concurrent._
import play.api.test.PlaySpecification
import play.api.libs.ws.Response
import scala.concurrent.ExecutionContext.Implicits.global
import model.Stripe.StripeObject
import play.api.libs.json.Reads
import model.Stripe


class StripeServiceTest extends PlaySpecification {


  "SubscriptionService" should {

    "create customers" in {
      val expected = RequestInfo(
        url = "http://localhost:9999/v1/customers",
        body = Map("card" -> Seq("tok_104Bpz2eZvKYlo2CRWVWL4Ou")),
        header = ("Authorization", "Bearer test_api_secret")
      )
      TestStripeService(expected).Customer.create("tok_104Bpz2eZvKYlo2CRWVWL4Ou")
      1 mustEqual 1 //just to keep specs2 happy. The real assertion is in the TestStripeService
    }


    "should create a subscription" in {
      val expected = RequestInfo(
        url = s"http://localhost:9999/v1/customers/cust_123/subscriptions",
        body = Map("plan" -> Seq("Patron")),
        header = ("Authorization", "Bearer test_api_secret")
      )
      TestStripeService(expected).Subscription.create("cust_123", "Patron")
      1 mustEqual 1 //just to keep specs2 happy. The real assertion is in the TestStripeService
    }
  }

  case class RequestInfo(url: String, body: Map[String, Seq[String]], header: (String, String)*)

  class TestStripeService(expected: RequestInfo) extends StripeService {
    val apiURL = "http://localhost:9999/v1"
    val apiAuthHeader = ("Authorization", "Bearer test_api_secret")

    def get[A <: StripeObject](endpoint: String)(implicit reads: Reads[A]): Future[A] = {
      RequestInfo(s"$apiURL/$endpoint", Map.empty) mustEqual expected
      Future.failed[A](Stripe.Error("internal", "Not implemented")) // don't care
    }

    def post[A <: StripeObject](endpoint: String, data: Map[String, Seq[String]])(implicit reads: Reads[A]): Future[A] = {
      RequestInfo(s"$apiURL/$endpoint", data, apiAuthHeader) mustEqual expected
      Future.failed[A](Stripe.Error("internal", "Not implemented")) // don't care
    }

    def delete[A <: StripeObject](endpoint: String)(implicit reads: Reads[A]): Future[A] = {
      RequestInfo(s"$apiURL/$endpoint", Map[String, Seq[String]](), apiAuthHeader) mustEqual expected
      Future.failed[A](Stripe.Error("internal", "Not implemented")) // don't care
    }
  }

  object TestStripeService {
    def apply(expected: RequestInfo) = new TestStripeService(expected)
  }
}


