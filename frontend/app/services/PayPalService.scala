package services

import com.gu.okhttp.RequestRunners
import com.gu.paypal.PayPalConfig
import com.netaporter.uri.QueryString
import com.netaporter.uri.Uri.parseQuery
import com.typesafe.scalalogging.LazyLogging
import configuration.Config
import controllers.PayPal.{PayPalBillingDetails, Token}
import okhttp3.{FormBody, Request, Response}

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

class PayPalService(apiConfig: PayPalConfig, implicit val executionContext: ExecutionContext) extends LazyLogging {

  val config = apiConfig
  // The parameters sent with every NVP request.
  val defaultNVPParams = Map(
    "USER" -> config.user,
    "PWD" -> config.password,
    "SIGNATURE" -> config.signature,
    "VERSION" -> config.NVPVersion)

  // Logs the result of the PayPal NVP request.
  private def logNVPResponse(response: QueryString) = {

    def msg(status: String) = s"PayPal: $status (NVPResponse: $response)"

    retrieveNVPParam(response, "ACK") match {
      case "Success" => logger.info("Successful PayPal NVP request")
      case "SuccessWithWarning" => logger.warn(msg("Warning"))
      case "Failure" => logger.error(msg("Error"))
      case "FailureWithWarning" => logger.error(msg("Error With Warning"))
    }

  }

  // Extracts response params as a map.
  private def extractResponse(response: Response) = {

    val responseBody = response.body().string()

    if (Config.stageDev)
      logger.info("NVP response body = " + responseBody)

    val parsedResponse = parseQuery(responseBody)

    logNVPResponse(parsedResponse)
    parsedResponse

  }

  // Takes a series of parameters, send a request to PayPal, returns response.
  private def nvpRequest(params: Map[String, String]) = {

    val reqBody = new FormBody.Builder()
    for ((param, value) <- defaultNVPParams) reqBody.add(param, value)
    for ((param, value) <- params) reqBody.add(param, value)

    val request = new Request.Builder()
      .url(config.url)
      .post(reqBody.build())
      .build()

    for {
      response <- RequestRunners.configurableFutureRunner(20.seconds)(ExecutionContext.Implicits.global)(request)
    } yield extractResponse(response)
  }

  // Takes an NVP response and retrieves a given parameter as a string.
  private def retrieveNVPParam(response: QueryString, paramName: String) =
    response.paramMap(paramName).head

  def retrieveEmail(baid: String) = {
    val params = Map(
      "METHOD" -> "BillAgreementUpdate",
      "REFERENCEID" -> baid
    )

    for {
      resp <- nvpRequest(params)
    } yield retrieveNVPParam(resp, "EMAIL")
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

    for {
      resp <- nvpRequest(paymentParams)
    } yield retrieveNVPParam(resp, "TOKEN")
  }

  // Sends a request to PayPal to create billing agreement and returns BAID.
  def retrieveBaid(token: Token) = {
    val agreementParams = Map(
      "METHOD" -> "CreateBillingAgreement",
      "TOKEN" -> token.token)

    for {
      resp <- nvpRequest(agreementParams)
    } yield retrieveNVPParam(resp, "BILLINGAGREEMENTID")
  }
}
