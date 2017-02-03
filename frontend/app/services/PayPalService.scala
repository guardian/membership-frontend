package services

import com.gu.okhttp.RequestRunners
import com.gu.paypal.PayPalConfig
import com.netaporter.uri.Uri.parseQuery
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import controllers.PayPal.{PayPalBillingDetails, Token}
import okhttp3.{FormBody, Request, Response}

class PayPalService(apiConfig: PayPalConfig) extends LazyLogging {

  val config = apiConfig
  // The parameters sent with every NVP request.
  val defaultNVPParams = Map(
    "USER" -> config.user,
    "PWD" -> config.password,
    "SIGNATURE" -> config.signature,
    "VERSION" -> config.NVPVersion)

  // Takes a series of parameters, send a request to PayPal, returns response.
  private def nvpRequest(params: Map[String, String]) = {

    val reqBody = new FormBody.Builder()
    for ((param, value) <- defaultNVPParams) reqBody.add(param, value)
    for ((param, value) <- params) reqBody.add(param, value)

    val request = new Request.Builder()
      .url(config.url)
      .post(reqBody.build())
      .build()

    RequestRunners.client.newCall(request).execute()
  }

  // Takes an NVP response and retrieves a given parameter as a string.
  private def retrieveNVPParam(response: Response, paramName: String) = {
    val responseBody = response.body().string()
    if (Config.stageDev)
      logger.info("NVP response body = " + responseBody)

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
  def retrieveToken(returnUrl : String, cancelUrl : String)(billingDetails: PayPalBillingDetails) = {
    val paymentParams = Map(
      "METHOD" -> "SetExpressCheckout",
      "PAYMENTREQUEST_0_PAYMENTACTION" -> "SALE",
      "L_PAYMENTREQUEST_0_NAME0" -> s"Guardian ${billingDetails.tier.capitalize}",
      "L_PAYMENTREQUEST_0_DESC0" -> s"You have chosen the ${billingDetails.billingPeriod} payment option",
      "L_PAYMENTREQUEST_0_AMT0" -> s"${billingDetails.amount}",
      "PAYMENTREQUEST_0_AMT" -> s"${billingDetails.amount}",
      "PAYMENTREQUEST_0_CURRENCYCODE" -> s"${billingDetails.currency}",
      "RETURNURL" -> returnUrl,
      "CANCELURL" -> cancelUrl,
      "BILLINGTYPE" -> "MerchantInitiatedBilling",
      "NOSHIPPING" -> "1")

    val response = nvpRequest(paymentParams)
    retrieveNVPParam(response, "TOKEN")
  }

  // Sends a request to PayPal to create billing agreement and returns BAID.
  def retrieveBaid(token: Token) = {
    logger.debug("Called retrieveBaid")

    val agreementParams = Map(
      "METHOD" -> "CreateBillingAgreement",
      "TOKEN" -> token.token)

    val response = nvpRequest(agreementParams)
    retrieveNVPParam(response, "BILLINGAGREEMENTID")
  }
}
