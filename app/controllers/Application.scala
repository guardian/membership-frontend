package controllers

import model._
import model.EventbriteDeserializer._
import play.api._
import play.api.mvc._
import com.stripe._
import com.stripe.model._
import scala.collection.convert.wrapAll._
import play.api.data._
import play.api.data.Forms._
import play.api.libs.ws._
import scala.concurrent._
import play.api.libs.concurrent.Execution.Implicits.defaultContext




object Application extends Controller {

  case class StripePayment(token: String)

  val stripePaymentForm = Form(
    mapping(
      "stripeToken" -> nonEmptyText
    )(StripePayment.apply)(StripePayment.unapply)
  )

  def getEventsList:Future[Seq[EBEvent]] = {

    val url = "https://www.eventbriteapi.com/v3/users/99154249965/owned_events"

    val request = WS.url(url).withQueryString(("token", "***REMOVED***"))

    for {
      response <- request.get()
    } yield {
      response.json.as[EBResponse].events
    }
  }

  def stripe = Action {
    Ok(views.html.stripe())
  }

  def stripeSubmit = Action { implicit request =>
    Logger.info(s"Request body = ${request.body}")

    Stripe.apiKey = "***REMOVED***"

    //val cardToken = request.queryString.get("stripeToken").flatMap(_.headOption)

    stripePaymentForm.bindFromRequest.fold(
      formWithErrors => {
        // binding failure, you retrieve the form containing errors:
        BadRequest
      },
      stripePayment => {
        val chargeParams = Map[String, Object](
          ("amount", 400: java.lang.Integer),
          ("currency", "usd"),
          ("card", stripePayment.token),
          ("description", "Charge for test@example.com")
        )

        Logger.info(chargeParams.toString)

        Charge.create(chargeParams)
        Ok("Subscription successful")
      }
    )

  }

}
