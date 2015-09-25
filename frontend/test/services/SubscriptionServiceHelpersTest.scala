package services

import org.specs2.mutable.Specification
import utils.Resource
import com.gu.membership.zuora.soap.Readers._

class SubscriptionServiceHelpersTest extends Specification {

  def query(resource: String) = queryResultReader.read(Resource.get(resource)).right.get.results

  "SubscriptionServiceHelpers" should {
    "sort amendments by subscription version" in {
      val subscriptions = subscriptionReader.read(query("model/zuora/subscriptions.xml"))
      val amendments = amendmentReader.read(query("model/zuora/amendments.xml"))

      val sortedAmendments = SubscriptionService.sortAmendments(subscriptions, amendments)

      sortedAmendments(0).id mustEqual "2c92c0f847cdc31e0147cf24390d6ad7"
      sortedAmendments(1).id mustEqual "2c92c0f847cdc31e0147cf2439b76ae6"
    }

    "sort invoice items by charge number ascending" in {
      val invoiceItems = invoiceItemReader.read(query("model/zuora/invoice-result.xml"))

      val sortedInvoiceItems = SubscriptionService.sortInvoiceItems(invoiceItems)

      sortedInvoiceItems(0).id mustEqual "2c92c0f94b34f993014b4a1c3b0104b7"
      sortedInvoiceItems(1).id mustEqual "2c92c0f94b34f993014b4a1c3b0004b6"
    }

    "sort preview invoice items by price ascending" in {
      val amendResult = amendResultReader.read(Resource.get("model/zuora/amend-result-preview.xml")).right.get

      val sortedInvoiceItems = SubscriptionService.sortPreviewInvoiceItems(amendResult.invoiceItems)

      sortedInvoiceItems(0).price mustEqual -135f
      sortedInvoiceItems(1).price mustEqual 540f

    }
  }
}
