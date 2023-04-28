package com.gu.zuora.soap.models.errors

import scala.xml.Node

sealed trait ErrorType
case object ApiDisabled extends ErrorType
case object CannotDelete extends ErrorType
case object CreditCardProcessingFailure extends ErrorType
case object DuplicateValue extends ErrorType
case object InvalidField extends ErrorType
case object InvalidId extends ErrorType
case object InvalidLogin extends ErrorType
case object InvalidSession extends ErrorType
case object InvalidType extends ErrorType
case object InvalidValue extends ErrorType
case object InvalidVersion extends ErrorType
case object LockCompetition extends ErrorType
case object MalformedQuery extends ErrorType
case object MaxRecordsExceeded extends ErrorType
case object MissingRequiredValue extends ErrorType
case object RequestExceededLimit extends ErrorType
case object RequestExceededRate extends ErrorType
case object ServerUnavailable extends ErrorType
case object TemporaryError extends ErrorType
case object TransactionFailed extends ErrorType
case object TransactionTerminated extends ErrorType
case object TransactionTimeout extends ErrorType
case object UnknownError extends ErrorType

sealed trait PaymentGatewayErrorType extends ErrorType
case object Fraudulent extends PaymentGatewayErrorType
case object TransactionNotAllowed extends PaymentGatewayErrorType
case object DoNotHonor extends PaymentGatewayErrorType
case object InsufficientFunds extends PaymentGatewayErrorType
case object RevocationOfAuthorization extends PaymentGatewayErrorType
case object GenericDecline extends PaymentGatewayErrorType
case object UnknownPaymentError extends PaymentGatewayErrorType

sealed trait Error extends Throwable {
  val message: String
}

case class XmlParseError(msg: String) extends Error {
  override val message = msg
}

case class QueryError(msg: String) extends Error {
  override val message = msg
}

// https://knowledgecenter.zuora.com/DC_Developers/SOAP_API/L_Error_Handling
trait ZuoraError extends Error {
  val code: String
  override def getMessage: String = s"$code: $message"
}

// https://knowledgecenter.zuora.com/DC_Developers/SOAP_API/L_Error_Handling/Faults
case class ZuoraFault(faultcode: String, faultstring: String) extends ZuoraError {
  override val code = faultcode
  override val message = faultstring
}

// https://knowledgecenter.zuora.com/DC_Developers/SOAP_API/L_Error_Handling/Errors
case class ZuoraPartialError(code: String, message: String, `type`: ErrorType) extends ZuoraError

// Error after stripe authorisation
case class PaymentGatewayError(
    val errorCode: String,
    val msg: String,
    gatewayResponse: String,
    gatewayResponseCode: String,
    errType: PaymentGatewayErrorType = UnknownPaymentError) extends ZuoraError {
  override val code = errorCode
  override val message = msg
}

object ErrorHandler {
  def apply(result: Node): Error = {
    if (!(result \ "GatewayResponse").isEmpty) {
      handlePaymentGatewayResponseError(result)
    } else if (!(result \ "faultcode").isEmpty) {
      handleZuoraFaults(result)
    } else {
      handleZuoraPartialErrors(result)
    }
  }

  private def handlePaymentGatewayResponseError(result: Node) = {
    val code = (result \ "Errors" \ "Code").head.text
    val message = (result \ "Errors" \ "Message").head.text

    val `type` =  message match {
      case "Transaction declined.insufficient_funds - Your card has insufficient funds." => InsufficientFunds
      case "Transaction declined.fraudulent - Your card was declined." => Fraudulent
      case "Transaction declined.transaction_not_allo - Your card does not support this type of purchase." => TransactionNotAllowed
      case "Transaction declined.do_not_honor - Your card was declined." => DoNotHonor
      case "Transaction declined.revocation_of_author - Your card was declined." => RevocationOfAuthorization
      case "Transaction declined.generic_decline - Your card was declined." => GenericDecline
      case _ => UnknownPaymentError
    }

    PaymentGatewayError(
      code,
      message,
      (result \ "GatewayResponse").head.text,
      (result \ "GatewayResponseCode").head.text,
      `type`)
  }

  private def handleZuoraFaults(result: Node) =
    ZuoraFault((result \ "faultcode").text, (result \ "faultstring").text)

  private  def handleZuoraPartialErrors(result: Node) = {
    val code = (result \ "Errors" \ "Code").head.text
    val message = (result \ "Errors" \ "Message").head.text

    val `type` =  code match {
      case "API_DISABLED" => ApiDisabled
      case "CANNOT_DELETE" => CannotDelete
      case "CREDIT_CARD_PROCESSING_FAILURE" => CreditCardProcessingFailure
      case "DUPLICATE_VALUE" => DuplicateValue
      case "INVALID_FIELD" => InvalidField
      case "INVALID_ID" => InvalidId
      case "INVALID_LOGIN" => InvalidLogin
      case "INVALID_SESSION" => InvalidSession
      case "INVALID_TYPE" => InvalidType
      case "INVALID_VALUE" => InvalidValue
      case "INVALID_VERSION" => InvalidVersion
      case "LOCK_COMPETITION" => LockCompetition
      case "MALFORMED_QUERY" => MalformedQuery
      case "MAX_RECORDS_EXCEEDED" => MaxRecordsExceeded
      case "MISSING_REQUIRED_VALUE" => MissingRequiredValue
      case "REQUEST_EXCEEDED_LIMIT" => RequestExceededLimit
      case "REQUEST_EXCEEDED_RATE" => RequestExceededRate
      case "SERVER_UNAVAILABLE" => ServerUnavailable
      case "TRANSACTION_FAILED" => TransactionFailed
      case "TRANSACTION_TERMINATED" => TransactionTerminated
      case "TRANSACTION_TIMEOUT" => TransactionTimeout
      case "UNKNOWN_ERROR" => UnknownError
    }

    ZuoraPartialError(code, message, `type`)
  }
}