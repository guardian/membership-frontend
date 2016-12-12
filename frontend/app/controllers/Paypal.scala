package controllers

import play.api.mvc.{Controller, Action}
import play.api.mvc.Results.BadRequest
import play.api.libs.json.{Json, JsSuccess, JsError}
import okhttp3.{OkHttpClient, FormBody, Request, Response}
import com.netaporter.uri.Uri.parseQuery
import configuration.Config

class Paypal extends Controller {

	// Payment token used to tie Paypal requests together.
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

	// Takes a series of parameters, send a request to Paypal, returns response.
	def nvpRequest (params: Map[String, String]) = {

		val client = new OkHttpClient()
		val reqBody = new FormBody.Builder()

		for ((param, value) <- defaultNVPParams) reqBody.add(param, value)
		for ((param, value) <- params) reqBody.add(param, value)

		val request = new Request.Builder()
			.url(Config.paypalSandboxUrl)
			.post(reqBody.build())
			.build()

		client.newCall(request).execute()

	}

	// Takes an NVP response and retrieves a given parameter as a string.
	def retrieveNVPParam (response: Response, paramName: String) = {

		val responseBody = response.body().string()
		val queryParams = parseQuery(responseBody)
		queryParams.paramMap.get(paramName).get(0)

	}

	// Retrieves a payment token from an NVP response, and wraps it in JSON for
	// sending back to the client.
	def tokenJsonResponse (response: Response) = {

		val token = Token(retrieveNVPParam(response, "TOKEN"))
		Json.toJson(token)

	}

	// Sends a request to Paypal to create billing agreement and returns BAID.
	def retrieveBaid (token: Token) = {

		val agreementParams = Map(
			"METHOD" -> "CreateBillingAgreement",
			"TOKEN" -> token.token)

		val response = nvpRequest(agreementParams)
		retrieveNVPParam(response, "BILLINGAGREEMENTID")

	}

	// Sets up a payment by contacting Paypal, returns the token as JSON.
	def setupPayment = Action {

		val paymentParams = Map(
			"METHOD" -> "SetExpressCheckout",
			"PAYMENTREQUEST_0_PAYMENTACTION" -> "SALE",
			"PAYMENTREQUEST_0_AMT" -> "4.50",
			"PAYMENTREQUEST_0_CURRENCYCODE" -> "GBP",
			"RETURNURL" -> "http://localhost:9000/create-agreement",
			"CANCELURL" -> "http://localhost:9000/cancel",
			"BILLINGTYPE" -> "MerchantInitiatedBilling")

		val response = nvpRequest(paymentParams)
		Ok(tokenJsonResponse(response))

	}

	// Creates a billing agreement using a payment token.
	def createAgreement = Action { request =>

		request.body.asJson.map { json =>

			Json.fromJson[Token](json) match {
				case JsSuccess(token: Token, _) => Ok(retrieveBaid(token))
				case e: JsError => BadRequest(JsError.toJson(e).toString)
			}

		}.getOrElse(BadRequest)

	}

}
