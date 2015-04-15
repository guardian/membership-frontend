package services

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
}
