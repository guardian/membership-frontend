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

  private val chargeAmount: Integer = 400

  def stripe = Action {
    Ok(views.html.stripe())
  }

  def stripeSubmit = Action {
    implicit request =>
      stripePaymentForm.bindFromRequest.fold(ifBindingFailsReturnBadRequest, ifBindingSucceedsMakePayment)
  }

  private case class StripePayment(token: String)

  private val stripePaymentForm = Form(mapping("stripeToken" -> nonEmptyText)(StripePayment.apply)(StripePayment.unapply))

  private def ifBindingFailsReturnBadRequest: (Form[StripePayment]) => Status = formWithErrors => BadRequest

  private def ifBindingSucceedsMakePayment: (StripePayment) => Status = {
    stripePayment => {
      val chargeParams = Map[String, Object](
        "amount" -> chargeAmount,
        "currency" -> "usd",
        "card" -> stripePayment.token,
        "description" -> "Charge for test@example.com")

      Logger.debug(chargeParams.toString)
      Charge.create(chargeParams)
      Ok
    }
  }

}
