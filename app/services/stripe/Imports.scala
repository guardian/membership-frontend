package services.stripe

import scala.concurrent.Future

import com.typesafe.config.ConfigFactory

import model.stripe._

object Imports {

  object Stripe extends StripeService {
    private val config = ConfigFactory.load()

    protected val apiURL = config.getString("stripe.api.url")
    protected val apiSecret = config.getString("stripe.api.secret")

    object charge {
      def create(amount: Int, currency: String, card: String, description: String): Future[Charge] = {
        post[Charge]("charges", Map(
          "amount" -> Seq(amount.toString),
          "currency" -> Seq(currency),
          "card" -> Seq(card),
          "description" -> Seq(description)
        ))
      }
    }

    object customer {
      def create(card: String): Future[Customer] =
        post[Customer]("customers", Map("card" -> Seq(card)))
    }

    object subscription {
      def create(customerId: String, planId: String): Future[Subscription] =
        post[Subscription](s"customers/$customerId", Map("plan" -> Seq(planId)))
    }

  }
}

