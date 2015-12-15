package services

import com.gu.config.Membership
import com.gu.zuora.{ZuoraService, rest}
import com.gu.zuora.soap.ClientWithFeatureSupplier
import com.gu.zuora.soap.models.Queries.{InvoiceItem, Subscription, Amendment}
import org.specs2.mutable.Specification
import org.joda.time.{DateTime, DurationFieldType}
import com.github.nscala_time.time.Imports._

class ZuoraServiceTest extends Specification {
  val service = new ZuoraService(???, ???, ???)

  "findCurrentSubscriptionStatus" in {
    val now = DateTime.now()
    def version(v: Int): Subscription = Subscription(
      id=v.toString,
      name="name",
      accountId="accountId",
      version=v,
      termStartDate=now,
      termEndDate=now,
      contractAcceptanceDate=now,
      activationDate=Some(now))

    def amend(v: Int, contractEffectiveDate: DateTime): Amendment =
      Amendment(v.toString, "TEST", contractEffectiveDate, v.toString)

    "returns the latest subscription when no future amendments exists" in {
      service.findCurrentSubscriptionStatus(
        Seq(version(1), version(2)),
        Nil
      ).currentVersion mustEqual version(2)

      service.findCurrentSubscriptionStatus(
        Seq(version(1), version(2)),
        Seq(amend(1, now - 1.minutes))
      ).currentVersion mustEqual version(2)
    }
    "returns the latest subscription when future amendments exists" in {
      service.findCurrentSubscriptionStatus(
        Seq(version(1), version(2)),
        Seq(amend(1, now + 1.month))
      ).currentVersion mustEqual version(1)
    }
  }

  "latestInvoiceItems" should {
    def invoiceItem(subscriptionId: String, chargeNumber: String = "1") = {
      val start = LocalDate.now().toDateTimeAtStartOfDay()
      InvoiceItem("item-id", 1.2f, start,
        start.withFieldAdded(DurationFieldType.months(), 1),
        chargeNumber, "item", subscriptionId)
    }

    "return an empty list when given an empty list" in {
      service.latestInvoiceItems(Seq()) mustEqual Seq()
    }

    "return all those items when given many items with the same subscriptionId" in {
      val items = Seq(invoiceItem("a"), invoiceItem("a"), invoiceItem("a"))
      service.latestInvoiceItems(items) mustEqual items
    }

    "return items with the same subscriptionId as the newest item when given items with differing subscription ids" in {
      "items in date order" in {
        val items = Seq(invoiceItem("a", "1"), invoiceItem("b", "2"))
        service.latestInvoiceItems(items) mustEqual Seq(invoiceItem("b", "2"))
      }

      "items out of order" in {
        val items = Seq(invoiceItem("b", "1"), invoiceItem("a", "2"), invoiceItem("a", "3"), invoiceItem("c", "2"))
        service.latestInvoiceItems(items) mustEqual Seq(invoiceItem("a", "2"), invoiceItem("a", "3"))
      }
    }
  }
}
