package services.stripe

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import com.typesafe.config.ConfigFactory

import model.Stripe._

object Imports {

  // Expose Either.left and Either.right
  implicit class FutureEither[L, R](future: Future[Either[L, R]]) {
    object right {
      def flatMap[R2](block: R => Future[Either[L, R2]]) = future.flatMap {
        case Left(l) => Future.successful(Left(l))
        case Right(r) => block.apply(r)
      }
    }

    object left {
      def flatMap[L2](block: L => Future[Either[L2, R]]) = future.flatMap {
        case Left(l) => block.apply(l)
        case Right(r) => Future.successful(Right(r))
      }
    }
  }

  object Stripe extends StripeService {
    private val config = ConfigFactory.load()

    protected val apiURL = config.getString("stripe.api.url")
    protected val apiSecret = config.getString("stripe.api.secret")

    object charge {
      def create(amount: Int, currency: String, card: String, description: String): Future[Either[Error, Charge]] = {
        post[Charge]("charges", Map(
          "amount" -> Seq(amount.toString),
          "currency" -> Seq(currency),
          "card" -> Seq(card),
          "description" -> Seq(description)
        ))
      }
    }

    object customer {
      def create(card: String): Future[Either[Error, Customer]] =
        post[Customer]("customers", Map("card" -> Seq(card)))
    }

    object subscription {
      def create(customerId: String, planId: String): Future[Either[Error, Subscription]] =
        post[Subscription](s"customers/$customerId", Map("plan" -> Seq(planId)))
    }

  }
}

