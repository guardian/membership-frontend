package services

import org.specs2.mutable.BeforeAfter
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import scala.io.Source
import scala.concurrent.duration._
import scala.concurrent._
import play.api.test.PlaySpecification
import model.Stripe.{SubscriptionList, CardList, Card}


class StripeServiceIntegrationTest extends PlaySpecification {

  val Port = 8080
  val Host = "localhost"
  val token = "***REMOVED***"

  trait StubServer extends BeforeAfter {
    val wireMockServer = new WireMockServer(wireMockConfig().port(Port))

    def before = wireMockServer.start()

    def after = wireMockServer.stop()
  }

  "SubscriptionService" should {
    "create customers" in new StubServer {
      val path = "/v1/customers"

      stubFor(post(urlEqualTo(path))
        .withRequestBody(WireMock.equalTo(s"card=$token"))
        .willReturn(
          aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/json;charset=utf-8")
            .withBody(loadFile("../customers.json"))))

      val customerFuture = TestStripeService.createCustomer(token)

      val customer = Await.result(customerFuture, 2 seconds)
      customer.id mustEqual ("cus_4Bt317qP0aKefO")
      customer.cards mustEqual (CardList(List(Card("Visa", "4242"))))
      customer.subscriptions mustEqual (SubscriptionList(Nil))
    }
  }

  def loadFile(fileName: String): String = Source.fromInputStream(getClass.getResourceAsStream(fileName)).mkString

}


object TestStripeService extends StripeService {
  override protected val apiURL: String = "http://localhost:8080/v1"
  override protected val apiSecret: String = "test_api_secret"
}


