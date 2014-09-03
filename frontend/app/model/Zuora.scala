package model

import scala.xml.Node
import org.joda.time.DateTime

object Zuora {
  trait ZuoraObject

  case class Authentication(token: String, url: String) extends ZuoraObject

  case class Error(origin: String, code: String, message: String) extends ZuoraObject

  case class AmendResult(id: Seq[String]) extends ZuoraObject
  case class CreateResult(id: String) extends ZuoraObject
  case class QueryResult(results: Seq[Map[String, String]]) extends ZuoraObject
  case class SubscribeResult(id: String) extends ZuoraObject
  case class UpdateResult(id: String) extends ZuoraObject

  case class SubscriptionStatus(current: String, future: Option[String])

  case class SubscriptionDetails(planName: String, planAmount: Float, startDate: DateTime, endDate: DateTime,
                                 ratePlanId: String) {
    // TODO: is there a better way?
    val annual = endDate == startDate.plusYears(1)
  }

  object SubscriptionDetails {
    def apply(ratePlan: Map[String, String], ratePlanCharge: Map[String, String]): SubscriptionDetails = {
      val startDate = new DateTime(ratePlanCharge("EffectiveStartDate"))
      val endDate = ratePlanCharge.get("ChargedThroughDate").fold(DateTime.now)(new DateTime(_))

      SubscriptionDetails(ratePlan("Name"), ratePlanCharge("Price").toFloat, startDate, endDate, ratePlan("Id"))
    }
  }
}

object ZuoraReaders {
  import Zuora.{Error, ZuoraObject}

  trait ZuoraReader[T <: ZuoraObject] {
    val responseTag: String
    val multiResults = false

    def read(node: Node): Either[Error, T] = {
      val body = scala.xml.Utility.trim((scala.xml.Utility.trim(node) \ "Body").head)

      (body \ "Fault").headOption.map { fault =>
        Left(Error("fault", (fault \ "faultcode").text, (fault \ "faultstring").text))
      }.getOrElse {
        val resultNode = if (multiResults) "results" else "result"
        val result = body \ responseTag \ resultNode
        extract(result.head)
      }
    }

    protected def extract(result: Node): Either[Error, T]
  }

  object ZuoraReader {
    def apply[T <: ZuoraObject](tag: String)(extractFn: Node => Either[Error, T]) = new ZuoraReader[T] {
      val responseTag = tag
      protected def extract(result: Node): Either[Error, T] = extractFn(result)
    }
  }

  trait ZuoraResultReader[T <: ZuoraObject] extends ZuoraReader[T] {
    protected def extract(result: Node): Either[Error, T] = {
      if ((result \ "Success").text == "true") {
        Right(extract2(result))
      } else {
        val errors = result \ "Error" // TODO
        Left(Error("error", "UNSUCCESSFUL", ""))
      }
    }

    protected def extract2(result: Node): T
  }

  object ZuoraResultReader {
    def create[T <: ZuoraObject](tag: String, multi: Boolean, extractFn: Node => T) = new ZuoraResultReader[T] {
      val responseTag = tag
      override val multiResults: Boolean = multi
      protected def extract2(result: Node) = extractFn(result)
    }

    def apply[T <: ZuoraObject](tag: String)(extractFn: Node => T) = create(tag, multi=false, extractFn)
    def multi[T <: ZuoraObject](tag: String)(extractFn: Node => T) = create(tag, multi=true, extractFn)
  }
}

object ZuoraDeserializer {
  import Zuora._
  import ZuoraReaders._

  implicit val authenticationReader = ZuoraReader("loginResponse") { result =>
    Right(Authentication((result \ "Session").text, (result \ "ServerUrl").text))
  }

  implicit val amendResultReader = ZuoraResultReader.multi("amendResponse") { result =>
    AmendResult((result \ "AmendmentIds").map(_.text))
  }

  implicit val createResultReader = ZuoraResultReader("createResponse") { result =>
    CreateResult((result \ "Id").text)
  }

  implicit val queryResultReader = ZuoraReader("queryResponse") { result =>
    if ((result \ "done").text == "true") {
      val records =
        // Zuora still returns a records node even if there were no results
        if ((result \ "size").text.toInt == 0) {
          Nil
        } else {
          (result \ "records").map { record =>
            record.child.map { node => (node.label, node.text)}.toMap
          }
        }

      Right(QueryResult(records))
    } else {
      Left(Error("internal", "NOT_DONE", "The query was not complete (we don't support iterating query results)"))
    }
  }

  implicit val subscribeResultReader = ZuoraResultReader("subscribeResponse") { result =>
    SubscribeResult((result \ "SubscriptionId").text)
  }

  implicit val updateResultReader = ZuoraResultReader("updateResponse") { result =>
    UpdateResult((result \ "Id").text)
  }
}
