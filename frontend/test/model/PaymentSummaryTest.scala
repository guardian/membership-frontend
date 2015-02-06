package model

import model.Zuora.PaymentSummary
import model.ZuoraDeserializer._
import org.specs2.mutable.Specification
import services.SubscriptionServiceHelpers
import utils.Resource

class PaymentSummaryTest extends Specification {

  def query(resource: String) = queryResultReader.read(Resource.get(resource)).right.get.results

  "PaymentSummary" should {
    "give the total payment for an sequence of invoice items" in {
      val invoiceItems = invoiceItemReader.read(query("model/zuora/invoice-result.xml"))

      val paymentSummary = PaymentSummary(invoiceItems)
      paymentSummary.current.id mustEqual "2c92c0f94b34f993014b4a1c3b0004b6"
      paymentSummary.previous.size mustEqual 1
      paymentSummary.previous(0).id mustEqual "2c92c0f94b34f993014b4a1c3b0104b7"
      paymentSummary.totalPrice mustEqual 405f
    }
  }

}
