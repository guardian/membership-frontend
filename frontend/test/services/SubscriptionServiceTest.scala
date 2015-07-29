package services

import com.gu.membership.model.{FriendTierPlan, PaidTierPlan}
import com.gu.membership.salesforce.Tier
import com.gu.membership.salesforce.Tier.{Friend, Supporter, Partner, Patron}
import model.{Events, Books, BooksAndEvents}
import org.specs2.mutable.Specification
import model.Zuora.{Subscription, RatePlanCharge, RatePlan, SubscriptionDetails}
import org.joda.time.DateTime

class SubscriptionServiceTest extends Specification {
  "SubscriptionService" should {
    "extract an invoice from a map" in {
      val startDate = new DateTime(2014, 10, 6, 10, 0)
      val endDate = new DateTime(2014, 11, 7, 10, 0)

      val subscriptionDetails = SubscriptionDetails(
        Subscription("some id", 1, startDate, startDate),
        RatePlan("RatePlanId", "Product name - annual"),
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
      features(plan(Patron), Some(BooksAndEvents)) mustEqual List(
        feature1,
        feature2
      )
    }

    "return only one book or event for partner" in {
      features(plan(Partner), Some(Books)) mustEqual List(feature1)
      features(plan(Partner), Some(Events)) mustEqual List(feature2)
      features(plan(Partner), Some(BooksAndEvents)).size mustEqual 1
    }

    "return no features for supporters or friends" in {
      features(plan(Supporter), Some(Books)) mustEqual List.empty
      features(FriendTierPlan, Some(Events)) mustEqual List.empty
    }
  }
}
