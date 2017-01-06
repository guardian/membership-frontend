package controllers

import play.api.mvc.{Action, Controller}
import play.api.mvc.Results.BadRequest
import play.api.libs.json.{JsError, JsSuccess, Json}
import okhttp3.{FormBody, OkHttpClient, Request, Response}
import com.netaporter.uri.Uri.parseQuery
import configuration.Config
import com.typesafe.scalalogging.LazyLogging
import play.api.Logger

object PayPal extends Controller with LazyLogging {

	// Payment token used to tie PayPal requests together.
	case class Token (token: String)

	// Json writers.
	implicit val tokenWrites = Json.writes[Token]
	implicit val tokenReads = Json.reads[Token]

	// The parameters sent with every NVP request.
	private val defaultNVPParams = Map(
		"USER" -> Config.paypalUser,
		"PWD" -> Config.paypalPassword,
		"SIGNATURE" -> Config.paypalSignature,
		"VERSION" -> Config.paypalNVPVersion)

	// Takes a series of parameters, send a request to PayPal, returns response.
	private def nvpRequest (params: Map[String, String]) = {

		val client = new OkHttpClient()
		val reqBody = new FormBody.Builder()
		for ((param, value) <- defaultNVPParams) reqBody.add(param, value)
		for ((param, value) <- params) reqBody.add(param, value)

		val request = new Request.Builder()
			.url(Config.paypalUrl)
			.post(reqBody.build())
			.build()

		client.newCall(request).execute()

	}

	// Takes an NVP response and retrieves a given parameter as a string.
	private def retrieveNVPParam (response: Response, paramName: String) = {

		val responseBody = response.body().string()
		val queryParams = parseQuery(responseBody)
		queryParams.paramMap(paramName).head

	}

	// Retrieves a payment token from an NVP response, and wraps it in JSON for
	// sending back to the client.
	private def tokenJsonResponse (response: Response) = {

		val token = Token(retrieveNVPParam(response, "TOKEN"))
		Json.toJson(token)

	}

	// Sends a request to PayPal to create billing agreement and returns BAID.
	private def retrieveBaid (token: Token) = {
    logger.info("Called retrieveBaid")
		val agreementParams = Map(
			"METHOD" -> "CreateBillingAgreement",
			"TOKEN" -> token.token)

		val response = nvpRequest(agreementParams)
		Json.toJson(Token(retrieveNVPParam(response, "BILLINGAGREEMENTID")))

	}

	// Sets up a payment by contacting PayPal, returns the token as JSON.
	def setupPayment = NoCacheAction { request =>
    logger.info("Called setupPayment")
		val paymentParams = Map(
			"METHOD" -> "SetExpressCheckout",
			"PAYMENTREQUEST_0_PAYMENTACTION" -> "SALE",
			"PAYMENTREQUEST_0_AMT" -> "4.50",
			"PAYMENTREQUEST_0_CURRENCYCODE" -> "GBP",
			"RETURNURL" -> routes.PayPal.returnUrl().absoluteURL(true)(request),
			"CANCELURL" -> routes.PayPal.cancelUrl().absoluteURL(true)(request),
			"BILLINGTYPE" -> "MerchantInitiatedBilling")

		val response = nvpRequest(paymentParams)
		Ok(tokenJsonResponse(response))

	}

	// Creates a billing agreement using a payment token.
	def createAgreement = NoCacheAction { request =>

		request.body.asJson.map { json =>

			Json.fromJson[Token](json) match {
				case JsSuccess(token: Token, _) => Ok(retrieveBaid(token))
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
