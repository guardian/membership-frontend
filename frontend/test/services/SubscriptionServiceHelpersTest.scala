package services

import org.specs2.mutable.Specification
import utils.Resource
import model.ZuoraDeserializer._

class SubscriptionServiceHelpersTest extends Specification {

  def query(resource: String) = queryResultReader.read(Resource.get(resource)).right.get.results

  "SubscriptionServiceHelpers" should {
    "sort amendments by subscription version" in {
      val subscriptions = subscriptionReader.read(query("model/zuora/subscriptions.xml"))
      val amendments = amendmentReader.read(query("model/zuora/amendments.xml"))

      val sortedAmendments = SubscriptionServiceHelpers.sortAmendments(subscriptions, amendments)

      sortedAmendments(0).id mustEqual "2c92c0f847cdc31e0147cf24390d6ad7"
      sortedAmendments(1).id mustEqual "2c92c0f847cdc31e0147cf2439b76ae6"
    }

    "sort subscriptions by version" in {
      val subscriptions = subscriptionReader.read(query("model/zuora/subscriptions.xml"))

      val sortedSubscriptions = SubscriptionServiceHelpers.sortSubscriptions(subscriptions)

      sortedSubscriptions(0).id mustEqual "2c92c0f847cdc31e0147cf2111ba6173"
      sortedSubscriptions(1).id mustEqual "2c92c0f847cdc31e0147cf24396f6ae1"
      sortedSubscriptions(2).id mustEqual "2c92c0f847cdc31e0147cf243a166af0"
    }

    "sort accounts by created date" in {
      val accounts = accountReader.read(query("model/zuora/accounts.xml"))

      val sortedAccounts = SubscriptionServiceHelpers.sortAccounts(accounts)

      sortedAccounts(0).id mustEqual "2c92c0f9483f301e01485efe9af6743e"
      sortedAccounts(1).id mustEqual "2c92c0f8483f1ca401485f0168f1614c"
    }

    "sort invoice items by charge number ascending" in {
      val invoiceItems = invoiceItemReader.read(query("model/zuora/invoice-result.xml"))

      val sortedInvoiceItems = SubscriptionServiceHelpers.sortInvoiceItems(invoiceItems)

      sortedInvoiceItems(0).id mustEqual "2c92c0f94b34f993014b4a1c3b0104b7"
      sortedInvoiceItems(1).id mustEqual "2c92c0f94b34f993014b4a1c3b0004b6"
    }

    "sort preview invoice items by price ascending" in {
      val amendResult = amendResultReader.read(Resource.get("model/zuora/amend-result-preview.xml")).right.get

      val sortedInvoiceItems = SubscriptionServiceHelpers.sortPreviewInvoiceItems(amendResult.invoiceItems)

      sortedInvoiceItems(0).price mustEqual -135f
      sortedInvoiceItems(1).price mustEqual 540f

    }
  }
}
