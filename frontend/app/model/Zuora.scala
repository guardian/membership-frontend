package model

import scala.xml.Elem
import org.joda.time.DateTime

import com.gu.membership.salesforce.Tier

object Zuora {
  trait ZuoraObject

  case class Authentication(token: String, url: String) extends ZuoraObject

  case class PaymentMethod(id: String) extends ZuoraObject

  case class Query(results: Seq[Map[String, String]]) extends ZuoraObject

  case class Subscription(id: String) extends ZuoraObject

  case class SubscriptionStatus(current: String, future: Option[String])

  case class SubscriptionDetails(planName: String, planAmount: Float, startDate: DateTime, endDate: DateTime)
    extends ZuoraObject {
    // TODO: is there a better way?
    val annual = endDate == startDate.plusYears(1)
  }

  object SubscriptionDetails {
    def apply(map: Map[String, String]): SubscriptionDetails = {
      val startDate = new DateTime(map("EffectiveStartDate"))
      val endDate = map.get("ChargedThroughDate").fold(DateTime.now)(new DateTime(_))

      SubscriptionDetails(map("Name"), map("Price").toFloat, startDate, endDate)
    }
  }

  object Authentication {
    def apply(elem: Elem): Authentication = {
      val result = elem \\ "loginResponse" \ "result"
      Authentication((result \ "Session").text, (result \ "ServerUrl").text)
    }
  }

  object PaymentMethod {
    def apply(elem: Elem): PaymentMethod = {
      val result = elem \\ "createResponse" \ "result"
      PaymentMethod((result \ "Id").text)
    }
  }

  object Query {
    def apply(elem: Elem): Query = {
      val resultNode = elem \\ "queryResponse" \ "result"
      val size = (resultNode \ "size").text.toInt

      // Zuora still returns an empty records node even if there were no results
      if (size == 0) {
        Query(Nil)
      } else {
        val results = (resultNode \ "records").map { record =>
          record.child.map { node => (node.label, node.text)}.toMap
        }
        Query(results)
      }
    }
  }

  object Subscription {
    def apply(elem: Elem): Subscription = {
      val result = elem \\ "subscribeResponse" \ "result"
      Subscription((result \ "SubscriptionId").text)
    }
  }
}
