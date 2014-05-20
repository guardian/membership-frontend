package controllers

import play.api.mvc._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.data._
import play.api.data.Forms._

import services.stripe.Imports._
import scala.concurrent.Future

trait Subscription extends Controller {

  def stripe = Action {
    Ok(views.html.stripe())
  }

  def stripeSubmit = Action.async { implicit request =>
    stripePaymentForm.bindFromRequest
      .fold(_ => Future(BadRequest), makePayment)
  }

  private val stripePaymentForm =
    Form { single("stripeToken" -> nonEmptyText) }

  private def makePayment(stripeToken: String) = {
    Stripe.customer.create(stripeToken).right.flatMap { customer =>
      Stripe.subscription.create(customer.id, "test")
    }.map {
      case Left(error) => BadRequest(error.message)
      case Right(subscription) => Ok(subscription.id)
    }
  }
}

object Subscription extends Subscription
