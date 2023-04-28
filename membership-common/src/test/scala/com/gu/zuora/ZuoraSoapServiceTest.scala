package com.gu.zuora

import com.github.nscala_time.time.Imports._
import com.gu.zuora.soap.models.Queries.{Amendment, InvoiceItem, Subscription}
import org.joda.time.{DateTime, DurationFieldType}
import org.specs2.mutable.Specification

class ZuoraSoapServiceTest extends Specification {
  import ZuoraSoapService._

  "latestInvoiceItems" should {
    def invoiceItem(subscriptionId: String, chargeNumber: String = "1") = {
      val start = LocalDate.today()
      InvoiceItem("item-id", 1.2f, start,
        start.withFieldAdded(DurationFieldType.months(), 1),
        chargeNumber, "item", subscriptionId)
    }

    "return an empty list when given an empty list" in {
      latestInvoiceItems(Seq()) mustEqual Seq()
    }

    "return all those items when given many items with the same subscriptionId" in {
      val items = Seq(invoiceItem("a"), invoiceItem("a"), invoiceItem("a"))
      latestInvoiceItems(items) mustEqual items
    }

    "return items with the same subscriptionId as the newest item when given items with differing subscription ids" in {
      "items in date order" in {
        val items = Seq(invoiceItem("a", "1"), invoiceItem("b", "2"))
        latestInvoiceItems(items) mustEqual Seq(invoiceItem("b", "2"))
      }

      "items out of order" in {
        val items = Seq(invoiceItem("b", "1"), invoiceItem("a", "2"), invoiceItem("a", "3"), invoiceItem("c", "2"))
        latestInvoiceItems(items) mustEqual Seq(invoiceItem("a", "2"), invoiceItem("a", "3"))
      }
    }
  }
}
