package model

import org.specs2.mutable.Specification

import model.Zuora._
import model.ZuoraDeserializer._
import utils.Resource
import org.joda.time.DateTime

class ZuoraDeserializerTest extends Specification {
  "Authentication" should {
    "have the correct token and url on success" in {
      val login = authenticationReader.read(Resource.get("model/zuora/authentication-success.xml")).right.get

      login.token mustEqual "yiZutsU55oKFQDR28lv210d7iWh8FVW9K45xFeFWMov7OSlRRI0soZ40DHmdLokscEjaUOo7Jt4sFxm_QZPyAhJdpR9yIxi_"
      login.url mustEqual "https://apisandbox.zuora.com/apps/services/a/58.0"
    }
  }

  "any ZuoraResult" should {
    "return a FaultError" in {
      val error = authenticationReader.read(Resource.get("model/zuora/fault-error.xml")).left.get
      error mustEqual FaultError("fns:INVALID_VALUE", "Invalid login. User name and password do not match.")
      error.fatal must beTrue
    }

    "return a fatal ResultError" in {
      val error = subscribeResultReader.read(Resource.get("model/zuora/result-error-fatal.xml")).left.get
      error mustEqual ResultError("API_DISABLED", "The API was disabled.")
      error.fatal must beTrue
    }

    "return a TRANSACTION_FAILED error as a non-fatal ResultError" in {
      val error = subscribeResultReader.read(Resource.get("model/zuora/result-error-non-fatal.xml")).left.get
      error mustEqual ResultError("TRANSACTION_FAILED", "Transaction declined.generic_decline - Your card was declined.")
      error.fatal must beFalse
    }

    "return a XML_PARSE_ERROR InternalError if the XML is invalid" in {
      subscribeResultReader.read(Resource.get("model/zuora/invalid.xml")).left.get match {
        case error: InternalError => error.code mustEqual "XML_PARSE_ERROR"
        case _ => ko("error is not of type InternalError")
      }
    }
  }

  "AmendResult" should {
    "have multiple ids" in {
      val amend = amendResultReader.read(Resource.get("model/zuora/amend-result.xml")).right.get

      amend.ids.length mustEqual 2
      amend.ids(0) mustEqual "2c92c0f847f1dcf00147fe0a50520707"
      amend.ids(1) mustEqual "2c92c0f847f1dcf00147fe0a50f3071f"
    }
  }

  "CreateResult" should {
    "have an id" in {
      val create = createResultReader.read(Resource.get("model/zuora/create-result.xml")).right.get
      create.id mustEqual "2c92c0f847ae39ba0147c580319a7208"
    }
  }

  "QueryResult" should {
    "have no results" in {
      val query = queryResultReader.read(Resource.get("model/zuora/query-empty.xml")).right.get
      query.results.size mustEqual 0
    }

    "have one result" in {
      val query = queryResultReader.read(Resource.get("model/zuora/query-single.xml")).right.get

      query.results.size mustEqual 1

      query.results(0)("Id") mustEqual "2c92c0f947cddc220147d3c765d0433e"
      query.results(0)("Version") mustEqual "1"
    }

    "have multiple results" in {
      val query = queryResultReader.read(Resource.get("model/zuora/subscriptions.xml")).right.get

      query.results.size mustEqual 3

      query.results(0)("Id") mustEqual "2c92c0f847cdc31e0147cf2111ba6173"
      query.results(0)("Version") mustEqual "1"

      query.results(1)("Id") mustEqual "2c92c0f847cdc31e0147cf243a166af0"
      query.results(1)("Version") mustEqual "3"

      query.results(2)("Id") mustEqual "2c92c0f847cdc31e0147cf24396f6ae1"
      query.results(2)("Version") mustEqual "2"
    }

    "not allow iterable queries" in {
      val error = queryResultReader.read(Resource.get("model/zuora/query-not-done.xml")).left.get
      error.code mustEqual "QUERY_ERROR"
    }
  }

  "SubscribeResult" should {
    "have an id on success" in {
      val subscribe = subscribeResultReader.read(Resource.get("model/zuora/subscribe-result.xml")).right.get
      subscribe.id mustEqual "8a80812a4733a5bb0147a1a4887f410a"
    }
  }

  "UpdateResult" should {
    "have an id" in {
      val update = updateResultReader.read(Resource.get("model/zuora/update-result.xml")).right.get
      update.id mustEqual "2c92c0f847ae39b80147c584947b7ea3"
    }
  }

  "ZuoraQueryReader" should {
    def query(resource: String) = queryResultReader.read(Resource.get(resource)).right.get.results

    "extract an Account" in {
      val accounts = accountReader.read(query("model/zuora/accounts.xml"))

      accounts.size mustEqual 2

      accounts(0) mustEqual Account("2c92c0f8483f1ca401485f0168f1614c", new DateTime("2014-09-10T03:00:00.000-08:00"))
      accounts(1) mustEqual Account("2c92c0f9483f301e01485efe9af6743e", new DateTime("2014-09-10T02:56:57.000-08:00"))
    }

    "extract an Amendment" in {
      val amendments = amendmentReader.read(query("model/zuora/amendments.xml"))

      amendments.size mustEqual 2

      amendments(0) mustEqual Amendment("2c92c0f847cdc31e0147cf2439b76ae6", "RemoveProduct",
        new DateTime("2015-08-13T12:29:15.000-08:00"), "2c92c0f847cdc31e0147cf24396f6ae1")
      amendments(1) mustEqual Amendment("2c92c0f847cdc31e0147cf24390d6ad7", "NewProduct",
        new DateTime("2015-08-13T12:29:15.000-08:00"), "2c92c0f847cdc31e0147cf2111ba6173")

    }

    "extract a RatePlan" in {
      val ratePlans = ratePlanReader.read(query("model/zuora/rateplans.xml"))

      ratePlans.size mustEqual 1

      ratePlans(0) mustEqual RatePlan("2c92c0f94878e828014879176ff831c4", "Partner - annual")
    }

    "extract a RatePlanCharge" in {
      val ratePlanCharges = ratePlanChargeReader.read(query("model/zuora/rateplancharges.xml"))

      ratePlanCharges.size mustEqual 1

      ratePlanCharges(0) mustEqual RatePlanCharge("2c92c0f94878e82801487917701931c5",
        Some(new DateTime("2015-09-15T03:34:10.000-08:00")), new DateTime("2014-09-15T03:34:10.000-08:00"), 135.0f)
    }

    "extract a Subscription" in {
      val subscriptions = subscriptionReader.read(query("model/zuora/subscriptions.xml"))

      subscriptions.size mustEqual 3

      subscriptions(0) mustEqual Subscription("2c92c0f847cdc31e0147cf2111ba6173", 1)
      subscriptions(1) mustEqual Subscription("2c92c0f847cdc31e0147cf243a166af0", 3)
      subscriptions(2) mustEqual Subscription("2c92c0f847cdc31e0147cf24396f6ae1", 2)
    }

    "extract an InvoiceItem" in {
      val invoiceItems = invoiceItemReader.read(query("model/zuora/invoice-result.xml"))

      invoiceItems.size mustEqual 2

      invoiceItems(0).id mustEqual "2c92c0f94b34f993014b4a1c3b0004b6"
      invoiceItems(1).id mustEqual "2c92c0f94b34f993014b4a1c3b0104b7"
    }
  }
}
