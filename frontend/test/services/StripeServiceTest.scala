package services

import play.api.test.PlaySpecification
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent._
import model.{Tier, Stripe}
import model.Stripe.StripeObject
import play.api.libs.json.Json
import scala.concurrent.duration._
import org.joda.time.DateTime
import scala.io.Source
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.client.WireMock

class StripeServiceTest extends PlaySpecification {

  def expectStripeCustomerResponse = {
    stubFor(post(urlEqualTo("/v1/customers"))
      .withHeader ("Authorization", WireMock.equalTo("Basic c2tfdGVzdF9CUW9raWtKT3ZCaUkySGxXZ0g0b2xmUTI6")) //
      .willReturn(
        aResponse().withStatus(200)
          .withHeader("Content-Type", "application/json;charset=utf-8")
          .withBody(loadFile("../customers.json"))))
  }

  def expectStripeSubscriptionResponse = {
    stubFor(post(urlEqualTo("/v1/customers/cus_4Bt317qP0aKefO/subscriptions"))
      .withHeader ("Authorization", WireMock.equalTo("Basic c2tfdGVzdF9CUW9raWtKT3ZCaUkySGxXZ0g0b2xmUTI6")) //***REMOVED***
      .willReturn(
        aResponse().withStatus(200)
          .withHeader("Content-Type", "application/json;charset=utf-8")
          .withBody(loadFile("../subscription.json"))))
  }

  "StripeService" should {

    "support subscription" in {
      val wireMockServer = new WireMockServer()
      wireMockServer.start()
      wireMockServer.isRunning mustEqual true

      expectStripeCustomerResponse
      expectStripeSubscriptionResponse


      val subscriptionFuture: Future[Stripe.Subscription] = for {
        token <- createToken
        customer <- StripeService.createCustomer(token.id)
        subscription <- StripeService.createSubscription(customer.id, "Patron")
      } yield subscription

      wireMockServer.stop()

      val subscription = Await.result(subscriptionFuture, 5 seconds)
      subscription.plan.tier mustEqual Tier.Patron

    }
  }


  def loadFile(fileName: String): String = Source.fromInputStream(getClass.getResourceAsStream(fileName)).mkString

  /*
  * This method is only used by this test, so putting it here.
  * Retrieving tokens is done client side
  */
  def createToken: Future[Token] = {
    implicit val readsToken = Json.reads[Token]
    val t = StripeService.post[Token](
      s"tokens",
      Map(
        "card[number]" -> Seq("4242424242424242"),
        "card[exp_month]" -> Seq("12"),
        "card[exp_year]" -> Seq((DateTime.now.getYear + 1).toString),
        "card[cvc]" -> Seq("123")
      )
    )
    println(t)
    t
  }

  case class Token(id: String) extends StripeObject

}


object TestStripeService extends StripeService{
  override protected val apiURL: String = "http://localhost/8080"
  override protected val apiSecret: String = "test_api_secret"
}
