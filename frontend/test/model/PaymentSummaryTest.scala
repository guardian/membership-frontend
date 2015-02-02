package model

import model.Zuora.PaymentSummary
import model.ZuoraDeserializer._
import org.specs2.mutable.Specification
import utils.Resource

class PaymentSummaryTest extends Specification {

  def query(resource: String) = queryResultReader.read(Resource.get(resource)).right.get.results

  "PaymentSummary" should {
    "give the total payment for an sequence of invoice items" in {
      val invoiceItems = invoiceItemReader.read(query("model/zuora/invoice-result.xml"))

      val paymentSummary = PaymentSummary(invoiceItems.head, invoiceItems.tail)
      paymentSummary.totalPrice mustEqual 405f

    }
  }

}
