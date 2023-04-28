package com.gu.zuora.rest

import okhttp3.{Response => OkHTTPResponse}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import scalaz.\/

object Readers {
  import Reads._
  def parseResponse[T : Reads](resp: OkHTTPResponse): Response[T] =
    parseResponse[T](Json.parse(resp.body().string()))

  def checkSuccess(j: JsValue): JsResult[Boolean] =
    (j \ "success").validate[Boolean].filter(JsError("success was false"))(_ == true)

  def parseResponse[T : Reads](json: JsValue): Response[T] =
    checkSuccess(json).flatMap(_ => json.validate[T]).map(\/.r[Failure].apply).recoverTotal(errs => json.validate[Failure].map(\/.l[T].apply).recoverTotal(
      _ => \/.l[T](Failure("None", errs.errors.toSeq.map { case (e, es) => Error(0, s"$e: ${es.mkString(", ")}") }))
    ))

  implicit val readUnit: Reads[Unit] = Reads.pure(())

  implicit val subscriptionStatus = new Reads[SubscriptionStatus] {
    override def reads(v: JsValue): JsResult[SubscriptionStatus] = v match {
      case JsString("Draft") => JsSuccess(Draft)
      case JsString("PendingActivation") => JsSuccess(PendingActivation)
      case JsString("PendingAcceptance") => JsSuccess(PendingAcceptance)
      case JsString("Active") => JsSuccess(Active)
      case JsString("Cancelled") => JsSuccess(Cancelled)
      case JsString("Expired") => JsSuccess(Expired)
      case other => JsError(s"Cannot parse a SubscriptionStatus from object $other")
    }
  }

  implicit val errorMsgReads: Reads[Error] = Json.reads[Error]
  implicit val failureReads: Reads[Failure] = (
    (JsPath \ "processId").read[String] and
    (JsPath \ "reasons").read[List[Error]]
  )(Failure.apply _)

  implicit val featureReads = Json.reads[Feature]
}


