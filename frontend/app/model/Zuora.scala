package model

import scala.xml.Elem
import org.joda.time.DateTime

import com.gu.membership.salesforce.Tier

object Zuora {
  trait ZuoraObject


  case class Authentication(token: String, url: String) extends ZuoraObject

  case class Query(results: Seq[Map[String, String]]) extends ZuoraObject

  case class Subscription(id: String) extends ZuoraObject

  case class InvoiceItem(planName: String, planAmount: Float, startDate: DateTime, endDate: DateTime) extends ZuoraObject

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
}
