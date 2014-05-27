package services.stripe

import scala.concurrent.Future

import com.typesafe.config.ConfigFactory

import model.stripe._
import configuration.Config

object Imports {

  object Stripe extends StripeService {

    protected val apiURL = Config.stripeApiURL
    protected val apiSecret = Config.stripeApiSecret

    object Charge {
      def create(amount: Int, currency: String, card: String, description: String): Future[Charge] = {
        post[Charge]("charges", Map(
          "amount" -> Seq(amount.toString),
          "currency" -> Seq(currency),
          "card" -> Seq(card),
          "description" -> Seq(description)
        ))
      }
    }

    object Customer {
      def create(card: String): Future[Customer] =
        post[Customer]("customers", Map("card" -> Seq(card)))
    }

    object Subscription {
      def create(customerId: String, planId: String): Future[Subscription] =
        post[Subscription](s"customers/$customerId", Map("plan" -> Seq(planId)))
    }

  }
}

