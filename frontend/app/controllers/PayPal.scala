package controllers

import play.api.mvc.{Action, Controller}
import play.api.mvc.Results.BadRequest
import play.api.libs.json.{JsError, JsSuccess, Json}
import okhttp3.{FormBody, OkHttpClient, Request, Response}
import com.netaporter.uri.Uri.parseQuery
import configuration.Config
import com.typesafe.scalalogging.LazyLogging
import play.api.Logger
import services.PayPalService

object PayPal extends Controller with LazyLogging {

	// Payment token used to tie PayPal requests together.
	case class Token (token: String)

	// Json writers.
	implicit val tokenWrites = Json.writes[Token]
	implicit val tokenReads = Json.reads[Token]

	// Retrieves a payment token from an NVP response, and wraps it in JSON for
	// sending back to the client.
	private def tokenJsonResponse (token : String) = {
		Json.toJson(Token(token))
	}

	// Sets up a payment by contacting PayPal, returns the token as JSON.
	def setupPayment = NoCacheAction { request =>
    logger.info("Called setupPayment")
		Ok(tokenJsonResponse(PayPalService.retrieveToken(request)))
	}

	// Creates a billing agreement using a payment token.
	def createAgreement = NoCacheAction { request =>
		request.body.asJson.map { json =>

			Json.fromJson[Token](json) match {
				case JsSuccess(token: Token, _) => Ok(tokenJsonResponse(PayPalService.retrieveBaid(token)))
				case e: JsError => BadRequest(JsError.toJson(e).toString)
			}

		}.getOrElse(BadRequest)

	}

	// The endpoint corresponding to the Paypal return url, hit if the user is
	// redirected and needs to come back.
	def returnUrl = NoCacheAction {

		logger.info("User hit the Paypal returnUrl.")
		Ok(views.html.paypal.errorPage())

	}

	// The endpoint corresponding to the Paypal cancel url, hit if the user is
	// redirected and the payment fails.
	def cancelUrl = NoCacheAction {

		logger.error("User hit the Paypal cancelUrl, something went wrong.")
		Ok(views.html.paypal.errorPage())

	}

}
