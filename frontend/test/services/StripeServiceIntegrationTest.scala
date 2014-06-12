package services

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock._
import com.github.tomakehurst.wiremock.core.WireMockConfiguration._
import scala.io.Source
import scala.concurrent.duration._
import scala.concurrent._
import play.api.test.PlaySpecification
import model.Stripe._
import org.specs2.specification.{Step, Fragments}
import model.Stripe.SubscriptionList
import model.Stripe.Card
import model.Stripe.CardList


class StripeServiceIntegrationTest extends BeforeAllAfterAll {
  val Port = 9999
  val Host = "localhost"
  val stripeApiSecret = "***REMOVED***"
  val cardToken = "tok_104Bpz2eZvKYlo2CRWVWL4Ou"
  val wireMockServer = new WireMockServer(wireMockConfig().port(Port))
  WireMock.configureFor(Host, Port)

  override protected def beforeAll(): Unit = wireMockServer.start()

  override protected def afterAll(): Unit = wireMockServer.stop()

  "SubscriptionService" should {

    "create customers" in {
      val given = Given(
        url = "/v1/customers",
        body = s"card=$cardToken",
        filePath = "../customers.json"
      )
      def when = TestStripeService.createCustomer(cardToken)
      val expectResult = Customer("cus_4Bt317qP0aKefO", SubscriptionList(Nil), CardList(List(Card("Visa", "4242"))))

      executeTest(given)(when)(expectResult)
    }

    "should create a subscription" in {
      val given = Given(
        url = s"/v1/customers/$stripeApiSecret/subscriptions",
        body = "plan=Patron",
        filePath = "../subscription.json"
      )
      def when = TestStripeService.createSubscription(stripeApiSecret, "Patron")
      val expectResult = Subscription(id = "sub_4Bt3MJQXzF3RG9", start = 1402329978, current_period_end = 1404921978, Plan("Patron", "Patron plan", 6000))

      executeTest(given)(when)(expectResult)
    }
  }

  def loadFile(fileName: String): String = Source.fromInputStream(getClass.getResourceAsStream(fileName)).mkString


  object TestStripeService extends StripeService {
    override protected val apiURL: String = s"http://localhost:$Port/v1"
    override protected val apiSecret: String = "test_api_secret"
  }

  case class Given(url: String, body: String, filePath: String)

  def executeTest(given: Given)(functionUnderTest: => Future[_])(expected: Any) = {
    stubFor(post(urlMatching(given.url))
      .withRequestBody(WireMock.equalTo(given.body))
      .withHeader("Authorization", WireMock.matching("Bearer test_api_secret*"))
      .willReturn(aResponse()
      .withStatus(200)
      .withHeader("Content-Type", "application/json;charset=utf-8")
      .withBody((loadFile(given.filePath)
      ))))

    val resultFuture = functionUnderTest
    val result = Await.result(resultFuture, 2 seconds)

    result mustEqual expected
  }

}


trait BeforeAllAfterAll extends PlaySpecification {
  override def map(fragments: => Fragments) =
    Step(beforeAll) ^ fragments ^ Step(afterAll)

  protected def beforeAll()

  protected def afterAll()
}


