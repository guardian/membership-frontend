package services

import play.api.test.PlaySpecification
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent._
import model.{Tier, Stripe}
import model.Stripe.StripeObject
import play.api.libs.json.Json
import scala.concurrent.duration._
import org.joda.time.DateTime

class StripeServiceTest extends PlaySpecification {

  "StripeService" should {

    "support subscription" in {
      val subscriptionFuture: Future[Stripe.Subscription] = for {
        token <- createToken
        customer <- StripeService.Customer.create(token.id)
        subscription <- StripeService.Subscription.create(customer.id, "Patron")
      } yield subscription

      val subscription = Await.result(subscriptionFuture, 5 seconds)
      subscription.plan.tier mustEqual Tier.Patron
    }
  }

  /*
  * This method is only used by this test, so putting it here.
  * Retrieving tokens is done client side
  */
  def createToken: Future[Token] = {
    implicit val readsToken = Json.reads[Token]
    StripeService.post[Token](
      s"tokens",
      Map(
        "card[number]" -> Seq("4242424242424242"),
        "card[exp_month]" -> Seq("12"),
        "card[exp_year]" -> Seq((DateTime.now.getYear + 1).toString),
        "card[cvc]" -> Seq("123")
      )
    )
  }

  case class Token(id: String) extends StripeObject
}