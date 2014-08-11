package model

import scala.xml.Elem
import org.joda.time.DateTime

import com.gu.membership.salesforce.Tier

object Zuora {
  trait ZuoraObject


  case class Authentication(token: String, url: String) extends ZuoraObject

  case class Query(results: Seq[Map[String, String]]) extends ZuoraObject

  case class Subscription(id: String) extends ZuoraObject

  case class InvoiceItem(planName: String, planAmount: Float, startDate: DateTime, endDate: DateTime) extends ZuoraObject {
    // TODO: is there a better way?
    val annual = endDate == startDate.plusYears(1)
  }

  object Authentication {
    def apply(elem: Elem): Authentication = {
      val result = elem \\ "loginResponse" \ "result"
      Authentication((result \ "Session").text, (result \ "ServerUrl").text)
    }
  }

  object Query {
    def apply(elem: Elem): Query = {
      val results = (elem \\ "queryResponse" \ "result" \ "records").map { record =>
        record.child.map { node => (node.label, node.text) }.toMap
      }
      Query(results)
    }
  }

  object Subscription {
    def apply(elem: Elem): Subscription = {
      val result = elem \\ "subscribeResponse" \ "result"
      Subscription((result \ "SubscriptionId").text)
    }
  }

  object InvoiceItem {
    def fromMap(invoice: Map[String, String]): InvoiceItem = {
      val startDate = new DateTime(invoice("ServiceStartDate"))
      val endDate = new DateTime(invoice("ServiceEndDate")).plusDays(1) // Yes we really have to +1 day
      val planAmount = invoice("ChargeAmount").toFloat + invoice("TaxAmount").toFloat

      InvoiceItem(invoice("ProductName"), planAmount, startDate, endDate)
    }
  }

}
