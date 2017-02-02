package services

import actions.AuthRequest
import com.gu.identity.play.IdMinimalUser
import com.gu.paypal.PayPalConfig
import com.gu.touchpoint.TouchpointBackendConfig.BackendType
import com.netaporter.uri.Uri.parseQuery
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import controllers.PayPal.{PayPalBillingDetails, Token}
import controllers.routes
import okhttp3.{FormBody, OkHttpClient, Request, Response}
import play.api.mvc.AnyContent
import utils.TestUsers

object PayPalService extends LazyLogging {

  // The parameters sent with every NVP request.
  private def defaultNVPParams(requestConfig : PayPalConfig) = Map(
    "USER" -> requestConfig.user,
    "PWD" -> requestConfig.password,
    "SIGNATURE" -> requestConfig.signature,
    "VERSION" -> requestConfig.NVPVersion)

  // Takes a series of parameters, send a request to PayPal, returns response.
  private def nvpRequest(params: Map[String, String], user : IdMinimalUser) = {
    logger.info("Is test user = " + TestUsers.isTestUser(user))
    val requestConfig = TouchpointBackend.forUser(user).payPalConfig

    val client = new OkHttpClient()
    val reqBody = new FormBody.Builder()
    for ((param, value) <- defaultNVPParams(requestConfig)) reqBody.add(param, value)
    for ((param, value) <- params) reqBody.add(param, value)

    val request = new Request.Builder()
      .url(requestConfig.url)
      .post(reqBody.build())
      .build()

    client.newCall(request).execute()
  }

  // Takes an NVP response and retrieves a given parameter as a string.
  private def retrieveNVPParam(response: Response, paramName: String) = {
    val responseBody = response.body().string()
    if (Config.stageDev)
      logger.debug("NVP response body = " + responseBody)

    val queryParams = parseQuery(responseBody)
    queryParams.paramMap(paramName).head
  }

  def retrieveEmail(baid: String, user: IdMinimalUser) = {
    val params = Map(
      "METHOD" -> "BillAgreementUpdate",
      "REFERENCEID" -> baid
    )

    val response = nvpRequest(params, user)
    retrieveNVPParam(response, "EMAIL")
  }

  // Sets up a payment by contacting PayPal and returns the token.
  def retrieveToken(request: AuthRequest[AnyContent], billingDetails: PayPalBillingDetails) = {
    val paymentParams = Map(
      "METHOD" -> "SetExpressCheckout",
      "PAYMENTREQUEST_0_PAYMENTACTION" -> "SALE",
      "L_PAYMENTREQUEST_0_NAME0" -> s"Guardian ${billingDetails.tier.capitalize}",
      "L_PAYMENTREQUEST_0_DESC0" -> s"You have chosen the ${billingDetails.billingPeriod} payment option",
      "L_PAYMENTREQUEST_0_AMT0" -> s"${billingDetails.amount}",
      "PAYMENTREQUEST_0_AMT" -> s"${billingDetails.amount}",
      "PAYMENTREQUEST_0_CURRENCYCODE" -> s"${billingDetails.currency}",
      "RETURNURL" -> routes.PayPal.returnUrl().absoluteURL(secure = true)(request),
      "CANCELURL" -> routes.PayPal.cancelUrl().absoluteURL(secure = true)(request),
      "BILLINGTYPE" -> "MerchantInitiatedBilling",
      "NOSHIPPING" -> "1")

    val response = nvpRequest(paymentParams, request.user)
    retrieveNVPParam(response, "TOKEN")
  }

  // Sends a request to PayPal to create billing agreement and returns BAID.
  def retrieveBaid(request: AuthRequest[AnyContent], token: Token) = {
    logger.debug("Called retrieveBaid")

    val agreementParams = Map(
      "METHOD" -> "CreateBillingAgreement",
      "TOKEN" -> token.token)

    val response = nvpRequest(agreementParams, request.user)
    retrieveNVPParam(response, "BILLINGAGREEMENTID")
  }
}
