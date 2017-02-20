package controllers

import actions.AuthRequest
import com.typesafe.scalalogging.LazyLogging
import play.api.libs.json.{JsError, JsSuccess, Json, Reads}
import play.api.mvc.{AnyContent, Controller}

object PayPal extends Controller with LazyLogging with PayPalServiceProvider {

  // Payment token used to tie PayPal requests together.
  case class Token(token: String)

  case class PayPalBillingDetails(amount: Float, billingPeriod: String, currency: String, tier: String)

  // Json writers.
  implicit val tokenWrites = Json.writes[Token]
  implicit val tokenReads = Json.reads[Token]
  implicit val billingDetails = Json.reads[PayPalBillingDetails]

  // Wraps the PayPal token and converts it to JSON
  // for sending back to the client.
  private def tokenJsonResponse(token: String) = {
    Json.toJson(Token(token))
  }

  // Sets up a payment by contacting PayPal, returns the token as JSON.
  def setupPayment = AuthenticatedAction { implicit request =>
    val returnUrl = routes.PayPal.returnUrl().absoluteURL(secure = true)(request)
    val cancelUrl = routes.PayPal.cancelUrl().absoluteURL(secure = true)(request)
    parseJsonAndRunServiceCall(request, payPalService.retrieveToken(returnUrl, cancelUrl))
  }

  // Creates a billing agreement using a payment token.
  def createAgreement = AuthenticatedAction { implicit request =>
    parseJsonAndRunServiceCall(request, payPalService.retrieveBaid)
  }

  //Takes a request, parses it into a type T, passes this into serviceCall to retrieve a token then returns this as json
  def parseJsonAndRunServiceCall[T](request: AuthRequest[AnyContent], serviceCall: (T) => String)(implicit fjs: Reads[T]) = {
    request.body.asJson.map { json =>

      Json.fromJson[T](json)(fjs) match {
        case JsSuccess(parsed, _) => Ok(tokenJsonResponse(serviceCall(parsed)))
        case e: JsError => BadRequest(JsError.toJson(e).toString)
      }

    }.getOrElse(BadRequest)
  }

  // The endpoint corresponding to the PayPal return url, hit if the user is
  // redirected and needs to come back.
  def returnUrl = NoCacheAction {

    logger.error("User hit the PayPal returnUrl.")
    Ok(views.html.paypal.errorPage())

  }

  // The endpoint corresponding to the PayPal cancel url, hit if the user is
  // redirected and the payment fails.
  def cancelUrl = NoCacheAction {

    logger.error("User hit the PayPal cancelUrl, something went wrong.")
    Ok(views.html.paypal.errorPage())

  }
}
