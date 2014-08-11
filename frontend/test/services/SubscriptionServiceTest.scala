package services

import org.specs2.mutable.Specification
import model.Zuora.InvoiceItem
import org.joda.time.DateTime

class SubscriptionServiceTest extends Specification {
  "SubscriptionService" should {
    "extract an invoice from a map" in {
      val startDate = new DateTime(2014, 10, 6, 10, 0)
      val endDate = new DateTime(2014, 11, 7, 10, 0)

      val invoice = InvoiceItem.fromMap(
        Map(
          "ServiceStartDate" -> "2014-10-06T10:00:00",
          "ServiceEndDate" -> "2014-11-06T10:00:00",
          "ChargeAmount" -> "10",
          "TaxAmount" -> "2",
          "ProductName" -> "Product name"
        )
      )

      invoice mustEqual InvoiceItem("Product name", 12.0f, startDate, endDate)
      invoice.annual mustEqual false
    }
  }
}
