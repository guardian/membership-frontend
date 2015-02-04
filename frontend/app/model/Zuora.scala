package model

import scala.util.{Success, Failure, Try}
import scala.xml.Node
import org.joda.time.DateTime

object Zuora {
  trait ZuoraResult

  case class Authentication(token: String, url: String) extends ZuoraResult

  case class AmendResult(ids: Seq[String]) extends ZuoraResult
  case class CreateResult(id: String) extends ZuoraResult
  case class QueryResult(results: Seq[Map[String, String]]) extends ZuoraResult
  case class SubscribeResult(id: String) extends ZuoraResult
  case class UpdateResult(id: String) extends ZuoraResult

  trait ZuoraQuery

  case class Account(id: String, createdDate: DateTime) extends ZuoraQuery
  case class Amendment(id: String, amendType: String, contractEffectiveDate: DateTime, subscriptionId: String)
    extends ZuoraQuery
  case class InvoiceItem(id: String, price: Float, serviceStartDate: DateTime, serviceEndDate: DateTime,
                         chargeNumber: String, productName: String) extends ZuoraQuery {
    val nextPaymentDate = serviceEndDate.plusDays(1)
    // TODO: is there a better way?
    val annual = nextPaymentDate == serviceStartDate.plusYears(1)
  }
  case class RatePlan(id: String, name: String) extends ZuoraQuery
  case class RatePlanCharge(id: String, chargedThroughDate: Option[DateTime], effectiveStartDate: DateTime,
                            price: Float) extends ZuoraQuery
  case class Subscription(id: String, version: Int) extends ZuoraQuery

  trait Error extends Throwable {
    val code: String
    val message: String

    val fatal = true

    override def getMessage: String = s"$code: $message"
  }

  case class FaultError(code: String, message: String) extends Error
  case class ResultError(code: String, message: String) extends Error {
    override val fatal = {
      val cardDeclined = code == "TRANSACTION_FAILED"
      !cardDeclined
    }
  }
  case class InternalError(code: String, message: String) extends Error

  case class SubscriptionStatus(current: String, future: Option[String], amendType: Option[String]) {
    val cancelled = amendType.exists(_ == "Cancellation")
  }

  case class SubscriptionDetails(planName: String, planAmount: Float, startDate: DateTime, endDate: DateTime,
                                 ratePlanId: String) {
    // TODO: is there a better way?
    val annual = endDate == startDate.plusYears(1)
  }

  object SubscriptionDetails {
    def apply(ratePlan: RatePlan, ratePlanCharge: RatePlanCharge): SubscriptionDetails = {
      val endDate = ratePlanCharge.chargedThroughDate.getOrElse(DateTime.now)

      // Zuora requires rate plan names to be unique, even though they are never used as identifiers
      // We want to show the same name for annual and monthly, so remove the " - annual" or " - monthly"
      val planName = ratePlan.name.split(" - ")(0)

      SubscriptionDetails(planName, ratePlanCharge.price, ratePlanCharge.effectiveStartDate, endDate, ratePlan.id)
    }
  }

  case class PaymentSummary(current: InvoiceItem, previous: Seq[InvoiceItem]) {
    val totalPrice = current.price + previous.map(_.price).sum
  }

  object PaymentSummary {
    def apply(items: Seq[InvoiceItem]): PaymentSummary = {
      val sortedInvoiceItems = items.sortBy(_.chargeNumber)
      PaymentSummary(sortedInvoiceItems.last, sortedInvoiceItems.dropRight(1))
    }
  }
}

object ZuoraReaders {
  import Zuora._

  trait ZuoraReader[T <: ZuoraResult] {
    val responseTag: String
    val multiResults = false

    def read(body: String): Either[Error, T] = {
      Try(scala.xml.XML.loadString(body)) match {
        case Failure(ex) => Left(InternalError("XML_PARSE_ERROR", ex.getMessage))

        case Success(node) =>
          val body = scala.xml.Utility.trim((scala.xml.Utility.trim(node) \ "Body").head)

          (body \ "Fault").headOption.fold {
            val resultNode = if (multiResults) "results" else "result"
            val result = body \ responseTag \ resultNode

            extractEither(result.head)
          } { fault =>
            Left(FaultError((fault \ "faultcode").text, (fault \ "faultstring").text))
          }
      }
    }

    protected def extractEither(result: Node): Either[Error, T]
  }

  object ZuoraReader {
    def apply[T <: ZuoraResult](tag: String)(extractFn: Node => Either[Error, T]) = new ZuoraReader[T] {
      val responseTag = tag
      protected def extractEither(result: Node): Either[Error, T] = extractFn(result)
    }
  }

  trait ZuoraResultReader[T <: ZuoraResult] extends ZuoraReader[T] {
    protected def extractEither(result: Node): Either[Error, T] = {
      if ((result \ "Success").text == "true") {
        Right(extract(result))
      } else {
        val errors = (result \ "Errors").map { node => ResultError((node \ "Code").text, (node \ "Message").text) }
        Left(errors.head) // TODO: return more than just the first error
      }
    }

    protected def extract(result: Node): T
  }

  object ZuoraResultReader {
    def create[T <: ZuoraResult](tag: String, multi: Boolean, extractFn: Node => T) = new ZuoraResultReader[T] {
      val responseTag = tag
      override val multiResults: Boolean = multi
      protected def extract(result: Node) = extractFn(result)
    }

    def apply[T <: ZuoraResult](tag: String)(extractFn: Node => T) = create(tag, multi=false, extractFn)
    def multi[T <: ZuoraResult](tag: String)(extractFn: Node => T) = create(tag, multi=true, extractFn)
  }

  trait ZuoraQueryReader[T <: ZuoraQuery] {
    val table: String
    val fields: Seq[String]

    def read(results: Seq[Map[String, String]]): Seq[T] = results.map(extract)

    def extract(result: Map[String, String]): T
  }

  object ZuoraQueryReader {
    def apply[T <: ZuoraQuery](tableName: String, fieldSeq: Seq[String])(extractFn: Map[String, String] => T) =
      new ZuoraQueryReader[T] {
        val table = tableName
        val fields = fieldSeq

        def extract(results: Map[String, String]) = extractFn(results)
      }
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
      Left(InternalError("QUERY_ERROR", "The query was not complete (we don't support iterating query results)"))
    }
  }

  implicit val subscribeResultReader = ZuoraResultReader("subscribeResponse") { result =>
    SubscribeResult((result \ "SubscriptionId").text)
  }

  implicit val updateResultReader = ZuoraResultReader("updateResponse") { result =>
    UpdateResult((result \ "Id").text)
  }

  implicit val accountReader = ZuoraQueryReader("Account", Seq("Id", "CreatedDate")) { result =>
    Account(result("Id"), new DateTime(result("CreatedDate")))
  }

  implicit val amendmentReader = ZuoraQueryReader("Amendment", Seq("Id", "Type", "ContractEffectiveDate", "SubscriptionId")) { result =>
    Amendment(result("Id"), result("Type"), new DateTime(result("ContractEffectiveDate")), result("SubscriptionId"))
  }

  implicit val invoiceItemReader = ZuoraQueryReader("InvoiceItem",
    Seq("Id", "ChargeAmount", "TaxAmount","ServiceStartDate", "ServiceEndDate", "ChargeNumber", "ProductName")) { result =>

    InvoiceItem(result("Id"), result("ChargeAmount").toFloat + result("TaxAmount").toFloat,
      new DateTime(result("ServiceStartDate")), new DateTime(result("ServiceEndDate")), result("ChargeNumber"),
      result("ProductName"))
  }

  implicit val ratePlanReader = ZuoraQueryReader("RatePlan", Seq("Id", "Name")) { result =>
    RatePlan(result("Id"), result("Name"))
  }

  implicit val ratePlanChargeReader = ZuoraQueryReader("RatePlanCharge", Seq("Id", "ChargedThroughDate", "EffectiveStartDate", "Price")) { result =>
    RatePlanCharge(result("Id"), result.get("ChargedThroughDate").map(new DateTime(_)),
      new DateTime(result("EffectiveStartDate")), result("Price").toFloat)
  }

  implicit val subscriptionReader = ZuoraQueryReader("Subscription", Seq("Id", "Version")) { result =>
    Subscription(result("Id"), result("Version").toInt)
  }
}
