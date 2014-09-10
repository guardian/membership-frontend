package services

import org.specs2.mutable.Specification
import utils.Resource
import model.ZuoraDeserializer.queryResultReader

class SubscriptionServiceHelpersTest extends Specification {

  "SubscriptionServiceHelpers" should {
    "sort amendments by subscription version" in {
      val subscriptions = queryResultReader.read(Resource.getXML("model/zuora/subscriptions.xml")).right.get.results
      val amendments = queryResultReader.read(Resource.getXML("model/zuora/amendments.xml")).right.get.results

      val sortedAmendments = SubscriptionServiceHelpers.sortAmendments(subscriptions, amendments)

      sortedAmendments(0)("Id") mustEqual "2c92c0f847cdc31e0147cf24390d6ad7"
      sortedAmendments(1)("Id") mustEqual "2c92c0f847cdc31e0147cf2439b76ae6"
    }

    "sort subscriptions by version" in {
      val subscriptions = queryResultReader.read(Resource.getXML("model/zuora/subscriptions.xml")).right.get.results

      val sortedSubscriptions = SubscriptionServiceHelpers.sortSubscriptions(subscriptions)

      sortedSubscriptions(0)("Id") mustEqual "2c92c0f847cdc31e0147cf2111ba6173"
      sortedSubscriptions(1)("Id") mustEqual "2c92c0f847cdc31e0147cf24396f6ae1"
      sortedSubscriptions(2)("Id") mustEqual "2c92c0f847cdc31e0147cf243a166af0"
    }

    "sort accounts by created date" in {
      val accounts = queryResultReader.read(Resource.getXML("model/zuora/accounts.xml")).right.get.results

      val sortedAccounts = SubscriptionServiceHelpers.sortAccounts(accounts)

      sortedAccounts(0)("Id") mustEqual "2c92c0f9483f301e01485efe9af6743e"
      sortedAccounts(1)("Id") mustEqual "2c92c0f8483f1ca401485f0168f1614c"
    }
  }
}
