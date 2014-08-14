package services

import org.specs2.mutable.Specification
import model.Zuora.SubscriptionDetails
import org.joda.time.DateTime

class SubscriptionServiceTest extends Specification {
  "SubscriptionService" should {
    "extract an invoice from a map" in {
      val startDate = new DateTime(2014, 10, 6, 10, 0)
      val endDate = new DateTime(2014, 11, 7, 10, 0)

      val subscriptionDetails = SubscriptionDetails(
        Map(
          "EffectiveStartDate" -> "2014-10-06T10:00:00",
          "ChargedThroughDate" -> "2014-11-06T10:00:00",
          "Price" -> "12",
          "Name" -> "Product name"
        )
      )

      subscriptionDetails mustEqual SubscriptionDetails("Product name", 12.0f, startDate, endDate)
      subscriptionDetails.annual mustEqual false
    }
  }
}
