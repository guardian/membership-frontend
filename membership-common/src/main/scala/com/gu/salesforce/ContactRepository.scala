package com.gu.salesforce

import com.gu.salesforce.ContactDeserializer._
import play.api.libs.json._

import scala.concurrent.{ExecutionContext, Future}
import scalaz.{-\/, EitherT, \/, \/-}
import scalaz.std.scalaFuture.futureInstance

abstract class ContactRepository(implicit ec: ExecutionContext) {

  val salesforce: Scalaforce

  def upsert(userId: Option[String], values: JsObject): Future[ContactId] = {
    for {
      result <- salesforce.Contact.upsert(userId.map(Keys.IDENTITY_ID -> _), values)
    } yield new ContactId {
      override def salesforceContactId: String = result.Id
      override def salesforceAccountId: String = result.AccountId
    }
  }

  import com.gu.memsub.subsv2.reads.Trace.{Traceable => T1}
  import com.gu.memsub.subsv2.services.Trace.Traceable

  private def toEither[A](j: JsResult[A]): String \/ A = j.fold({ errors =>
    \/.left[String, A](errors.toString)
  },\/.right)

  private def get(key: String, value: String): Future[String \/ Option[Contact]] = {
    salesforce.Contact.read(key, value).map { failableJsonContact =>
      (for {
        resultOpt <- failableJsonContact
        maybeContact <- resultOpt match {
          case Some(jsValue) =>
            toEither(jsValue.validate[Contact].withTrace(s"SF001: Invalid read Contact response from Salesforce for $key $value: $jsValue"))
              .map[Option[Contact]](Some.apply)
          case None => \/.r[String](None: Option[Contact])
        }
      } yield maybeContact).withTrace(s"SF002: could not get read contact response for $key $value")
    }
  }

  def get(identityId: String): Future[String \/ Option[Contact]] = // this returns right of None if the person isn't a member
    get(Keys.IDENTITY_ID, identityId)


}
