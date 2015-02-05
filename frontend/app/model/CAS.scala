package model

import play.api.libs.json.{JsPath, Json}

object CAS {
  trait CASResult

  case class CASError(message: String, code: Int) extends Throwable(s"CAS error - $code: $message") with CASResult

  case class CASSuccess(expiryType: String, provider: String, expiryDate: String, subscriptionCode: String,
                        content: String) extends CASResult

  object Deserializer {
    implicit val casErrorReads = (JsPath \ "error").read(Json.reads[CASError])
    implicit val casSuccessReads = (JsPath \ "expiry").read(Json.reads[CASSuccess])
  }
}
