package services

import com.netaporter.uri.Uri.parseQuery
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import controllers.PayPal.Token
import controllers.routes
import okhttp3.{FormBody, OkHttpClient, Request, Response}
import play.api.mvc.RequestHeader

object PayPalService extends LazyLogging {

  // The parameters sent with every NVP request.
  private val defaultNVPParams = Map(
    "USER" -> Config.paypalUser,
    "PWD" -> Config.paypalPassword,
    "SIGNATURE" -> Config.paypalSignature,
    "VERSION" -> Config.paypalNVPVersion)

  // Takes a series of parameters, send a request to PayPal, returns response.
  private def nvpRequest(params: Map[String, String]) = {

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
  private def retrieveNVPParam(response: Response, paramName: String) = {
    val responseBody = response.body().string()
    val queryParams = parseQuery(responseBody)
    queryParams.paramMap(paramName).head
  }

  def retrieveEmail(baid: String) = {
    val params = Map(
      "METHOD" -> "BillAgreementUpdate",
      "REFERENCEID" -> baid
    )

    val response = nvpRequest(params)
    retrieveNVPParam(response, "EMAIL")
  }

  // Sets up a payment by contacting PayPal and returns the token.
  def retrieveToken(request: RequestHeader) = {
    logger.info("Called setupPayment")
    val paymentParams = Map(
      "METHOD" -> "SetExpressCheckout",
      "PAYMENTREQUEST_0_PAYMENTACTION" -> "SALE",
      "PAYMENTREQUEST_0_AMT" -> "4.50",
      "PAYMENTREQUEST_0_CURRENCYCODE" -> "GBP",
      "RETURNURL" -> routes.PayPal.returnUrl().absoluteURL(secure = true)(request),
      "CANCELURL" -> routes.PayPal.cancelUrl().absoluteURL(secure = true)(request),
      "BILLINGTYPE" -> "MerchantInitiatedBilling")

    val response = nvpRequest(paymentParams)
    retrieveNVPParam(response, "TOKEN")
  }

  // Sends a request to PayPal to create billing agreement and returns BAID.
  def retrieveBaid(token: Token) = {
    logger.info("Called retrieveBaid")
    val agreementParams = Map(
      "METHOD" -> "CreateBillingAgreement",
      "TOKEN" -> token.token)

    val response = nvpRequest(agreementParams)
    retrieveNVPParam(response, "BILLINGAGREEMENTID")
  }

}
