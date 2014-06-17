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
        body = Map("card" -> Seq("tok_104Bpz2eZvKYlo2CRWVWL4Ou"))
      )
      TestStripeService(expected).Customer.create("tok_104Bpz2eZvKYlo2CRWVWL4Ou")
      1 mustEqual 1 //just to keep specs2 happy. The real assertion is in the TestStripeService
    }


    "should create a subscription" in {
      val expected = RequestInfo(
        url = s"http://localhost:9999/v1/customers/cust_123/subscriptions",
        body = Map("plan" -> Seq("Patron"))
      )
      TestStripeService(expected).Subscription.create("cust_123", "Patron")
      1 mustEqual 1 //just to keep specs2 happy. The real assertion is in the TestStripeService
    }

    "should cancel a subscription" in {
      val expected = RequestInfo(
        url = s"http://localhost:9999/v1/customers/cust_123/subscriptions/sub_123?at_period_end=true",
        body = Map.empty
      )

      TestStripeService(expected).Subscription.delete("cust_123", "sub_123")
      1 mustEqual 1 //just to keep specs2 happy. The real assertion is in the TestStripeService
    }

    "should update card details" in {
      val expected = RequestInfo(
        url = s"http://localhost:9999/v1/customers/cust_123",
        body = Map("card" -> Seq("tok_123"))
      )
      TestStripeService(expected).Customer.updateCard("cust_123", "tok_123")
      1 mustEqual 1 //just to keep specs2 happy. The real assertion is in the TestStripeService
    }
  }

  case class RequestInfo(url: String, body: Map[String, Seq[String]])

  class TestStripeService(expected: RequestInfo) extends StripeService {
    val apiURL = "http://localhost:9999/v1"

    def get[A <: StripeObject](endpoint: String)(implicit reads: Reads[A]): Future[A] = {
      RequestInfo(s"$apiURL/$endpoint", Map.empty) mustEqual expected
      Future.failed[A](Stripe.Error("internal", "Not implemented", None, None)) // don't care
    }

    def post[A <: StripeObject](endpoint: String, data: Map[String, Seq[String]])(implicit reads: Reads[A]): Future[A] = {
      RequestInfo(s"$apiURL/$endpoint", data) mustEqual expected
      Future.failed[A](Stripe.Error("internal", "Not implemented", None, None)) // don't care
    }

    def delete[A <: StripeObject](endpoint: String)(implicit reads: Reads[A]): Future[A] = {
      RequestInfo(s"$apiURL/$endpoint", Map.empty) mustEqual expected
      Future.failed[A](Stripe.Error("internal", "Not implemented", None, None)) // don't care
    }
  }

  object TestStripeService {
    def apply(expected: RequestInfo) = new TestStripeService(expected)
  }
}


