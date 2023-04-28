package com.gu.zuora.soap

import com.gu.i18n.Currency
import com.gu.i18n.Currency.GBP
import com.gu.zuora.soap.models.PaymentSummary
import com.gu.zuora.soap.models.Queries._
import com.gu.zuora.soap.models.Results._
import com.gu.zuora.soap.models.errors._
import com.gu.zuora.soap.readers.Reader
import org.joda.time.LocalDate
import org.specs2.mutable.Specification
import utils.Resource
import Readers._
import com.gu.zuora.api.StripeUKMembershipGateway

class DeserializerTest extends Specification {
  private implicit class ZuoraXmlFile(filename: String) {
    def as[T <: models.Result](implicit r: Reader[T]): Either[Error, T] =
      r.read(Resource.get(s"model/zuora/${filename}.xml"))
  }

  "Authentication" should {
    "have the correct token and url on success" in {
      val login = "authentication-success".as[Authentication].right.get

      login.token mustEqual "yiZutsU55oKFQDR28lv210d7iWh8FVW9K45xFeFWMov7OSlRRI0soZ40DHmdLokscEjaUOo7Jt4sFxm_QZPyAhJdpR9yIxi_"
      login.url mustEqual "https://apisandbox.zuora.com/apps/services/a/58.0"
    }
  }

  "any ZuoraResult" should {
    "return a FaultError" in {
      "fault-error".as[Authentication].left.get mustEqual ZuoraFault(
        "fns:INVALID_VALUE",
        "Invalid login. User name and password do not match.")
    }

    "return a API_DISABLED if API is not accessible " in {
      "result-error-fatal".as[SubscribeResult].left.get mustEqual ZuoraPartialError(
        "API_DISABLED",
        "The API was disabled.",
        ApiDisabled)
    }

    "return a TRANSACTION_FAILED if card payment is declined" in {
      "result-error-non-fatal".as[SubscribeResult].left.get mustEqual ZuoraPartialError(
        "TRANSACTION_FAILED",
        "Transaction declined.generic_decline - Your card was declined.",
        TransactionFailed)
    }

    "return XmlParseError if XML is invalid" in {
      "invalid".as[SubscribeResult].left.get match {
        case error: XmlParseError => ok("error is of correct type")
        case _ => ko("error is of wrong type")
      }
    }

    "return a PaymentGatewayError" in {
      "payment-gateway-error".as[SubscribeResult].left.get mustEqual PaymentGatewayError(
          "TRANSACTION_FAILED",
          "Transaction declined.generic_decline - Your card was declined.",
          "Your card was declined.",
          "Declined",
          GenericDecline)
    }
  }

  "AmendResult" should {
    "have multiple ids" in {
      val amend = "amend-result".as[AmendResult].right.get

      amend.ids.length mustEqual 2
      amend.ids(0) mustEqual "2c92c0f847f1dcf00147fe0a50520707"
      amend.ids(1) mustEqual "2c92c0f847f1dcf00147fe0a50f3071f"
      amend.invoiceItems.length mustEqual 0
    }

    "have preview invoice items" in {
      val amend = "amend-result-preview".as[AmendResult].right.get

      amend.ids.length mustEqual 1
      amend.invoiceItems.length mustEqual 2
      amend.invoiceItems(0).price mustEqual -135f
      amend.invoiceItems(1).price mustEqual 540f
    }
  }

  "CreateResult" should {
    "have an id" in {
      "create-result".as[CreateResult].right.get.id mustEqual "2c92c0f847ae39ba0147c580319a7208"
    }
  }

  "QueryResult" should {
    "have no results" in {
      "query-empty".as[QueryResult].right.get.results.size mustEqual 0
    }

    "have one result" in {
      val query = "query-single".as[QueryResult].right.get

      query.results.size mustEqual 1

      query.results(0)("Id") mustEqual "2c92c0f947cddc220147d3c765d0433e"
      query.results(0)("Version") mustEqual "1"
    }

    "have multiple results" in {
      val query = "subscriptions".as[QueryResult].right.get

      query.results(0)("Id") mustEqual "2c92c0f847cdc31e0147cf2111ba6173"
      query.results(0)("Version") mustEqual "1"

      query.results(1)("Id") mustEqual "2c92c0f847cdc31e0147cf243a166af0"
      query.results(1)("Version") mustEqual "2"

      query.results(2)("Id") mustEqual "2c92c0f847cdc31e0147cf24396f6ae1"
      query.results(2)("Version") mustEqual "3"
    }

    "not allow iterable queries" in {
      "query-not-done".as[QueryResult].left.get mustEqual QueryError(
        "The query was not complete (we don't support iterating query results)")
    }
  }

  "SubscribeResult" should {
    "have an id on success" in {
      val subscribe = "subscribe-result".as[SubscribeResult].right.get
      subscribe.subscriptionId mustEqual "8a80812a4733a5bb0147a1a4887f410a"
    }
  }

  "UpdateResult" should {
    "have an id" in {
      "update-result".as[UpdateResult].right.get.id mustEqual "2c92c0f847ae39b80147c584947b7ea3"
    }
  }

  "ZuoraQueryReader" should {
    def query(resource: String) = resource.as[QueryResult].right.get.results

    "extract an Account" in {
      val accounts = accountReader.read(query("accounts"))

      accounts.size mustEqual 2

      accounts(0) mustEqual Account("2c92c0f8483f1ca401485f0168f1614c", "BillToId", "SoldToId", 0, 0, Some(Currency.GBP), None, None, None)
      accounts(1) mustEqual Account("2c92c0f9483f301e01485efe9af6743e", "BillToId", "SoldToId", 31, 2.1f, Some(Currency.GBP), Some("1"), Some("003g0000010iD8CAAU"), Some(StripeUKMembershipGateway))
    }

    "extract an Amendment" in {
      val amendments = amendmentReader.read(query("amendments"))

      amendments.size mustEqual 2

      amendments(0) mustEqual Amendment("2c92c0f847cdc31e0147cf2439b76ae6", "RemoveProduct",
        new LocalDate("2015-08-13"), "2c92c0f847cdc31e0147cf24396f6ae1")
      amendments(1) mustEqual Amendment("2c92c0f847cdc31e0147cf24390d6ad7", "NewProduct",
        new LocalDate("2015-08-13"), "2c92c0f847cdc31e0147cf2111ba6173")

    }

    "extract a RatePlan" in {
      val ratePlans = ratePlanReader.read(query("rateplans"))

      ratePlans.size mustEqual 1

      ratePlans(0) mustEqual RatePlan("2c92c0f94878e828014879176ff831c4", "Partner - annual", "ProductRatePlanId")
    }

    "extract a RatePlanCharge" in {
      val ratePlanCharges = ratePlanChargeReader.read(query("rateplancharges"))

      ratePlanCharges.size mustEqual 1

      ratePlanCharges(0) mustEqual RatePlanCharge("2c92c0f94878e82801487917701931c5",
        Some(new LocalDate("2015-09-15")), new LocalDate("2014-09-15"),
        None, None, None, 135.0f)
    }

    "extract a Subscription" in {
      val subscriptions = subscriptionReader.read(query("subscriptions"))

      subscriptions.size mustEqual 3

      subscriptions(0) mustEqual Subscription("2c92c0f847cdc31e0147cf2111ba6173", "Sub1", "1", 1, new LocalDate("2015-04-01"), new LocalDate("2015-04-01"), new LocalDate("2015-10-01"), None)
      subscriptions(1) mustEqual Subscription("2c92c0f847cdc31e0147cf243a166af0", "Sub2", "1", 2, new LocalDate("2015-04-01"), new LocalDate("2015-04-01"), new LocalDate("2015-10-01"), None)
      subscriptions(2) mustEqual Subscription("2c92c0f847cdc31e0147cf24396f6ae1", "Sub3", "1", 3, new LocalDate("2015-04-01"), new LocalDate("2015-04-01"), new LocalDate("2015-10-01"), None)

    }

    "extract an InvoiceItem" in {
      val invoiceItems = invoiceItemReader.read(query("invoice-result"))

      invoiceItems.size mustEqual 2

      invoiceItems(0).id mustEqual "2c92c0f94b34f993014b4a1c3b0004b6"
      invoiceItems(1).id mustEqual "2c92c0f94b34f993014b4a1c3b0104b7"
    }

    "extract a rate plan" in {
      val invoiceItems = invoiceItemReader.read(query("invoice-result"))

      val paymentSummary = PaymentSummary(invoiceItems, GBP)
      paymentSummary.current.id mustEqual "2c92c0f94b34f993014b4a1c3b0004b6"
      paymentSummary.previous.size mustEqual 1
      paymentSummary.previous(0).id mustEqual "2c92c0f94b34f993014b4a1c3b0104b7"
      paymentSummary.totalPrice mustEqual 405f
    }
  }
}
