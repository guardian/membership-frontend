package services

import scala.concurrent._
import play.api.test.PlaySpecification
import play.api.libs.ws.Response
import scala.concurrent.ExecutionContext.Implicits.global


class StripeServiceTest extends PlaySpecification {


  "SubscriptionService" should {

    "create customers" in {
      val expected = RequestInfo(
        url = "http://localhost:9999/v1/customers",
        body = Map("card" -> Seq("tok_104Bpz2eZvKYlo2CRWVWL4Ou")),
        header = ("Authorization", "Bearer test_api_secret")
      )
      new TestStripeService(expected).createCustomer("tok_104Bpz2eZvKYlo2CRWVWL4Ou")
      1 mustEqual (1) //just to keep specs2 happy. The real assertion is in the TestStripeService
    }


    "should create a subscription" in {
      val expected = RequestInfo(
        url = s"http://localhost:9999/v1/customers/cust_123/subscriptions",
        body = Map("plan" -> Seq("Patron")),
        header = ("Authorization", "Bearer test_api_secret")
      )
      new TestStripeService(expected).createSubscription("cust_123", "Patron")
      1 mustEqual (1) //just to keep specs2 happy. The real assertion is in the TestStripeService
    }
  }

  case class RequestInfo(url: String, body: Map[String, Seq[String]], header: (String, String)*)

  class TestStripeService(expected: RequestInfo) extends StripeService {
    override protected val apiURL: String = s"http://localhost:9999/v1"
    override protected val apiSecret: String = "test_api_secret"

    override def httpPost(url: String, data: Map[String, Seq[String]], header: (String, String)*): Future[Response] = {
      RequestInfo(url, data, header: _*) mustEqual expected
      future(Response(null)) //don't care
    }

    override def httpGet(url: String, header: (String, String)*): Future[Response] = {
      RequestInfo(url, Map[String, Seq[String]](), header: _*) mustEqual expected
      future(Response(null)) //don't care
    }
  }

}


