package controllers

import com.gu.zuora.soap.models.errors._
import com.typesafe.scalalogging.LazyLogging
import controllers.Contributor.Forbidden
import play.api.libs.json.Json

trait PaymentGatewayErrorHandler extends LazyLogging {

  def handlePaymentGatewayError(e: PaymentGatewayError, userId: String, tier: String, tracking: List[(String, String)], country: String = "") = {

    def handleError(code: String) = {
      logger.warn(s"User $userId could not become $tier member due to payment gateway failed transaction: \n\terror=$e \n\tuser=$userId \n\ttracking=$tracking \n\tcountry=$country")
      Forbidden(Json.obj("type" -> "PaymentGatewayError", "code" -> code))
    }

    e.errType match {
      case Fraudulent => handleError("Fraudulent")
      case TransactionNotAllowed => handleError("TransactionNotAllowed")
      case DoNotHonor => handleError("DoNotHonor")
      case InsufficientFunds => handleError("InsufficientFunds")
      case RevocationOfAuthorization => handleError("RevocationOfAuthorization")
      case GenericDecline => handleError("GenericDecline")
      case _ => handleError("UknownPaymentError")
    }
  }

}
