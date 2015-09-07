package services

import com.gu.membership.model.{FriendTierPlan, PaidTierPlan}
import com.gu.membership.salesforce.Tier
import com.gu.membership.salesforce.Tier.{Friend, Supporter, Partner, Patron}
import model.{FreeEventTickets, Books}
import org.specs2.mutable.Specification
import model.Zuora._
import org.joda.time.DateTime

class SubscriptionServiceTest extends Specification {
  "SubscriptionService" should {
    "extract an invoice from a map" in {
      val startDate = new DateTime(2014, 10, 6, 10, 0)
      val endDate = new DateTime(2014, 11, 7, 10, 0)

      val subscriptionDetails = SubscriptionDetails(
        Subscription("some id", 1, startDate, startDate),
        RatePlan("RatePlanId", "Product name - annual", "productRatePlanId"),
        RatePlanCharge("RatePlanChargeId", Some(endDate), startDate, 12.0f)
      )

      subscriptionDetails mustEqual SubscriptionDetails("Product name", 12.0f, startDate, startDate, Some(endDate), "RatePlanId")
      subscriptionDetails.annual mustEqual false
    }
  }

  "featuresPerTier" should {
    import SubscriptionService.featuresPerTier
    import model.Zuora.{Feature => ZuoraFeature}

    val feature1 = ZuoraFeature(id="1", code="Books")
    val feature2 = ZuoraFeature(id="2", code="Events")
    val feature3 = ZuoraFeature(id="3", code="OtherFeature")

    val features = featuresPerTier(Seq(feature1, feature2, feature3)) _

    def plan(t: Tier) = PaidTierPlan(t, annual = true)

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
      features(FriendTierPlan, Set(FreeEventTickets)) mustEqual List.empty
    }
  }

  "findCurrentSubscriptionStatus" in {
    import SubscriptionService.findCurrentSubscriptionStatus

    val now = DateTime.now()
    def version(v: Int): Subscription = Subscription(v.toString, v, now, now )
    def amend(v: Int, contractEffectiveDate: DateTime): Amendment =
      Amendment(v.toString, "TEST", contractEffectiveDate, v.toString)

    "returns the latest subscription when no future amendments exists" in {
      findCurrentSubscriptionStatus(
        Seq(version(1), version(2)),
        Nil
      ).currentVersion mustEqual version(2)

      findCurrentSubscriptionStatus(
        Seq(version(1),version(2)),
        Seq(amend(1, now))
      ).currentVersion mustEqual version(2)
    }
    "returns the latest subscription when future amendments exists" in {
      findCurrentSubscriptionStatus(
        Seq(version(1),version(2)),
        Seq(amend(1, now.plusMonths(1)))
      ).currentVersion mustEqual version(1)

    }

  }
}
