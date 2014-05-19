package controllers

import play.api._
import play.api.mvc._
import com.stripe._
import com.stripe.model._
import scala.collection.convert.wrapAll._
import play.api.data._
import play.api.data.Forms._
import com.typesafe.config.ConfigFactory
import play.api.libs.json.Json
import scala.util.{ Failure, Success, Try }

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

  private def ifBindingFailsReturnBadRequest(formWithErrors: Form[StripePayment]) = BadRequest

  private def ifBindingSucceedsMakePayment(stripePayment: StripePayment) = {
    Logger.debug(stripePayment.asMap.toString)
    Try {
      Charge.create(stripePayment.asMap)
    } match {
      case Success(v) => Ok(Json.obj("status" -> 200))
      case Failure(e) => Ok(Json.obj("status" -> 400, "error" -> e.getMessage()))
    }
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
