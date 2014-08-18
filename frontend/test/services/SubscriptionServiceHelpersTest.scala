package services

import org.specs2.mutable.Specification
import utils.Resource
import model.Zuora.Query

class SubscriptionServiceHelpersTest extends Specification {

  "SubscriptionServiceHelpers" should {
    "sort amendments by subscription version" in {
      val subscriptions = Query(Resource.getXML("model/zuora/subscriptions.xml")).results
      val amendments = Query(Resource.getXML("model/zuora/amendments.xml")).results

      val sortedAmendments = SubscriptionServiceHelpers.sortAmendments(subscriptions, amendments)

      sortedAmendments(0)("Id") mustEqual "2c92c0f847cdc31e0147cf24390d6ad7"
      sortedAmendments(1)("Id") mustEqual "2c92c0f847cdc31e0147cf2439b76ae6"
    }

    "sort subscriptions by version" in {
      val subscriptions = Query(Resource.getXML("model/zuora/subscriptions.xml")).results

      val sortedSubscriptions = SubscriptionServiceHelpers.sortSubscriptions(subscriptions)

      sortedSubscriptions(0)("Id") mustEqual "2c92c0f847cdc31e0147cf2111ba6173"
      sortedSubscriptions(1)("Id") mustEqual "2c92c0f847cdc31e0147cf24396f6ae1"
      sortedSubscriptions(2)("Id") mustEqual "2c92c0f847cdc31e0147cf243a166af0"
    }
  }
}
