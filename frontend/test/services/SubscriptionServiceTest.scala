package services

import com.github.nscala_time.time.Imports._
import com.gu.membership.model._
import com.gu.membership.salesforce.PaidTier
import com.gu.membership.salesforce.Tier.{Partner, Patron, Supporter}
import com.gu.membership.zuora.soap.models.Queries._
import com.gu.membership.zuora.soap.models._
import model.{Books, FreeEventTickets}
import org.joda.time.format.DateTimeFormatter
import org.joda.time.{DateTime, DurationFieldType}
import org.specs2.mutable.Specification

class SubscriptionServiceTest extends Specification {
  "SubscriptionService" should {
    "extract an invoice from a map" in {
      val startDate = new DateTime(2014, 10, 6, 10, 0)
      val endDate = new DateTime(2014, 11, 7, 10, 0)

      val subscriptionDetails = SubscriptionDetails(
        Subscription(
          id = "some id",
          name = "name",
          accountId = "accountId",
          version = 1,
          termStartDate = startDate,
          termEndDate = startDate,
          contractAcceptanceDate = startDate,
          activationDate = Some(startDate)),
        RatePlan("RatePlanId", "Product name - annual", "ProductRatePlanId"),
        RatePlanCharge("RatePlanChargeId", Some(endDate), startDate, None, None, None, 12.0f)
      )


      subscriptionDetails mustEqual SubscriptionDetails("Product name", 12.0f, GBP, startDate, startDate, Some(endDate), "RatePlanId", "ProductRatePlanId")
      subscriptionDetails.annual mustEqual false
    }
  }

  "featuresPerTier" should {
    import SubscriptionService.featuresPerTier

    val feature1 = Feature(id = "1", code = "Books")
    val feature2 = Feature(id = "2", code = "Events")
    val feature3 = Feature(id = "3", code = "OtherFeature")

    val features = featuresPerTier(Seq(feature1, feature2, feature3)) _

    def plan(t: PaidTier) = PaidTierPlan.yearly(t, Current)

    "return both books and events for patrons" in {
      features(plan(Patron), Set(Books, FreeEventTickets)) mustEqual List(feature1, feature2)
      features(plan(Patron), Set()) mustEqual List(feature1, feature2)
    }

    "return only one book or event for partner" in {
      features(plan(Partner), Set(Books)) mustEqual List(feature1)
      features(plan(Partner), Set(FreeEventTickets)) mustEqual List(feature2)
      features(plan(Partner), Set(Books, FreeEventTickets)).size mustEqual 1
    }

    "return no features for supporters or friends" in {
      features(plan(Supporter), Set(Books)) mustEqual List.empty
      features(FriendTierPlan.current, Set(FreeEventTickets)) mustEqual List.empty
    }
  }

  "findCurrentSubscriptionStatus" in {
    import SubscriptionService.findCurrentSubscriptionStatus

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
      findCurrentSubscriptionStatus(
        Seq(version(1), version(2)),
        Nil
      ).currentVersion mustEqual version(2)

      findCurrentSubscriptionStatus(
        Seq(version(1), version(2)),
        Seq(amend(1, now - 1.minutes))
      ).currentVersion mustEqual version(2)
    }
    "returns the latest subscription when future amendments exists" in {
      findCurrentSubscriptionStatus(
        Seq(version(1), version(2)),
        Seq(amend(1, now + 1.month))
      ).currentVersion mustEqual version(1)
    }
  }


  "latestInvoiceItems" should {

    import SubscriptionService.latestInvoiceItems
    val formatter: DateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss")
    val april = formatter.parseDateTime("2015-04-01 09:00:00")
    val may = formatter.parseDateTime("2015-05-01 09:00:00")

    def invoiceItem(subscriptionId: String, start: DateTime) =
      InvoiceItem("item-id", 1.2f, start,
                  start.withFieldAdded(DurationFieldType.months(), 1),
                  "1", "item", subscriptionId)

    "return an empty list when given an empty list" in {
      latestInvoiceItems(Seq()) mustEqual Seq()
    }

    "return all those items when given many items with the same subscriptionId" in {
      val items = Seq(invoiceItem("a", april), invoiceItem("a", april), invoiceItem("a", april))
      latestInvoiceItems(items) mustEqual items
    }

    "return items with the same subscriptionId as the newest item when given items with differing subscription ids" in {
      "items in date order" in {
        val items = Seq(invoiceItem("a", april), invoiceItem("b", may))
        latestInvoiceItems(items) mustEqual Seq(invoiceItem("b", may))
      }

      "items out of order" in {
        val items = Seq(invoiceItem("b", april), invoiceItem("a", may), invoiceItem("a", april), invoiceItem("c", april))
        latestInvoiceItems(items) mustEqual Seq(invoiceItem("a", april), invoiceItem("a", may))
      }
    }
  }
}
