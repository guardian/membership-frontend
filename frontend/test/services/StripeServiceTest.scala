package services

import scala.concurrent._
import model.Stripe.StripeObject
import play.api.libs.json.Reads
import model.Stripe
import org.specs2.mutable.Specification


class StripeServiceTest extends Specification {

  "SubscriptionService" should {

    "create customers" in TestStripeService { service =>
      service.Customer.create("test@example.com", "tok_104Bpz2eZvKYlo2CRWVWL4Ou")
      service.lastRequest mustEqual RequestInfo(
        url = "http://localhost:9999/v1/customers",
        body = Map("email" -> Seq("test@example.com"), "card" -> Seq("tok_104Bpz2eZvKYlo2CRWVWL4Ou"))
      )
    }

    "should create a subscription" in TestStripeService { service =>
      service.Subscription.create("cust_123", "Patron")
      service.lastRequest mustEqual RequestInfo(
        url = s"http://localhost:9999/v1/customers/cust_123/subscriptions",
        body = Map("plan" -> Seq("Patron"))
      )
    }

    "should cancel a subscription" in TestStripeService { service =>
      service.Subscription.delete("cust_123", "sub_123")
      service.lastRequest mustEqual RequestInfo(
        url = s"http://localhost:9999/v1/customers/cust_123/subscriptions/sub_123?at_period_end=true",
        body = Map.empty
      )
    }

    "should update card details" in TestStripeService { service =>
      service.Customer.updateCard("cust_123", "tok_123")
      service.lastRequest mustEqual RequestInfo(
        url = s"http://localhost:9999/v1/customers/cust_123",
        body = Map("card" -> Seq("tok_123"))
      )
    }
  }

  class TestStripeService() extends StripeService {

    val apiURL = "http://localhost:9999/v1"

    var lastRequest = RequestInfo.empty

    def get[A <: StripeObject](endpoint: String)(implicit reads: Reads[A]): Future[A] = {
      lastRequest = RequestInfo(s"$apiURL/$endpoint", Map.empty)
      Future.failed[A](Stripe.Error("internal", "Not implemented", None, None)) // don't care
    }

    def post[A <: StripeObject](endpoint: String, data: Map[String, Seq[String]])(implicit reads: Reads[A]): Future[A] = {
      lastRequest = RequestInfo(s"$apiURL/$endpoint", data)
      Future.failed[A](Stripe.Error("internal", "Not implemented", None, None)) // don't care
    }

    def delete[A <: StripeObject](endpoint: String)(implicit reads: Reads[A]): Future[A] = {
      lastRequest = RequestInfo(s"$apiURL/$endpoint", Map.empty)
      Future.failed[A](Stripe.Error("internal", "Not implemented", None, None)) // don't care
    }
  }

  object TestStripeService {
    def apply[T](block: TestStripeService => T) = block(new TestStripeService)
  }
}


