package controllers

import play.api._
import play.api.mvc._
import com.stripe._
import com.stripe.model._
import scala.collection.convert.wrapAll._
import play.api.data._
import play.api.data.Forms._
import com.typesafe.config.ConfigFactory

object Subscription extends Subscription {
  Stripe.apiKey = ConfigFactory.load().getString("stripe.apiKey")
}

trait Subscription extends Controller {

  def stripe = Action {
    Ok(views.html.stripe())
  }

  def stripeSubmit = Action {
    implicit request =>
      stripePaymentForm.bindFromRequest.fold(ifBindingFailsReturnBadRequest, ifBindingSucceedsMakePayment)
  }

  private val stripePaymentForm: Form[StripePayment] =
    Form(mapping("stripeToken" -> nonEmptyText)(StripePayment.apply)(StripePayment.unapply))

  private def ifBindingFailsReturnBadRequest: (Form[StripePayment]) => Status = formWithErrors => BadRequest

  private def ifBindingSucceedsMakePayment: (StripePayment) => Status =
    stripePayment => {
      Logger.debug(stripePayment.asMap.toString)
      Charge.create(stripePayment.asMap)
      Ok
    }

  case class StripePayment(token: String) {
    val asMap: Map[String, Object] = Map(
      "amount" -> "400",
      "currency" -> "usd",
      "card" -> token,
      "description" -> "Charge for test@example.com"
    )
  }

}
