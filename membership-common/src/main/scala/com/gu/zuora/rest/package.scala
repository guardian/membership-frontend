package com.gu.zuora
import play.api.libs.json.{JsPath, Json, Reads}
import play.api.libs.functional.syntax._
import scalaz.\/

package object rest {

  type Response[T] = Failure \/ T
  case class Error(code: Int, message: String)
  case class Failure(processId: String, reasons: Seq[Error])

  sealed trait SubscriptionStatus
  case object Draft extends SubscriptionStatus
  case object PendingActivation extends SubscriptionStatus
  case object PendingAcceptance extends SubscriptionStatus
  case object Active extends SubscriptionStatus
  case object Cancelled extends SubscriptionStatus
  case object Expired extends SubscriptionStatus

  case class Feature(id: String, featureCode: String)
  case class ZuoraResponse(success: Boolean, error: Option[String] = None)
  implicit val zuoraResponseReads: Reads[ZuoraResponse] = (
    (JsPath \ "success").read[Boolean] and
    (JsPath \\ "message").readNullable[String]
    )(ZuoraResponse.apply _)

  case class ZuoraError(Code:String, Message:String)

  case class ZuoraCrudResponse(success: Boolean, errors: List[ZuoraError], createdId: Option[String] = None)


  implicit val ZuoraErrorReads = Json.reads[ZuoraError]
  implicit val ZuoraCreateResponseReads: Reads[ZuoraCrudResponse] = (
    (JsPath \ "Success").read[Boolean] and
    (JsPath \\ "Errors").read[List[ZuoraError]].orElse(Reads.pure(List.empty)) and
    (JsPath \ "Id").readNullable[String]
    )(ZuoraCrudResponse.apply _)

}
