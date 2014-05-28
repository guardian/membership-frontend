package controllers

import scala.concurrent.Future

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.data._
import play.api.data.Forms._

import services.stripe.Imports._
import model.stripe

trait Subscription extends Controller {

  def subscribe = Action.async { implicit request =>
    paymentForm.bindFromRequest
      .fold(_ => Future.successful(BadRequest), makePayment)
  }

  private val paymentForm =
    Form { tuple("stripeToken" -> nonEmptyText, "tier" -> nonEmptyText) }

  private def makePayment(formData: (String, String)) = {
    val (stripeToken, tier) = formData
    val payment = for {
      customer <- Stripe.Customer.create(stripeToken)
      subscription <- Stripe.Subscription.create(customer.id, tier)
    } yield Ok(subscription.id)

    payment.recover {
      case error: stripe.Error => BadRequest(error.message)
    }
  }
}

object Subscription extends Subscription
