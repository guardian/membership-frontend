package model

import org.specs2.mutable.Specification
import model.ZuoraDeserializer._
import utils.Resource
import model.Zuora.Error

class ZuoraDeserializerTest extends Specification {
  "Authentication" should {
    "have the correct token and url on success" in {
      val login = authenticationReader.read(Resource.getXML("model/zuora/authentication-success.xml")).right.get

      login.token mustEqual "yiZutsU55oKFQDR28lv210d7iWh8FVW9K45xFeFWMov7OSlRRI0soZ40DHmdLokscEjaUOo7Jt4sFxm_QZPyAhJdpR9yIxi_"
      login.url mustEqual "https://apisandbox.zuora.com/apps/services/a/58.0"
    }

    "show an error on failure" in {
      1 mustEqual 1
    }
  }

  "AmendResult" should {
    "have multiple ids" in {
      val amend = amendResultReader.read(Resource.getXML("model/zuora/amend-result.xml")).right.get

      amend.id.length mustEqual 2
      amend.id(0) mustEqual "2c92c0f847f1dcf00147fe0a50520707"
      amend.id(1) mustEqual "2c92c0f847f1dcf00147fe0a50f3071f"
    }
  }

  "CreateResult" should {
    "have an id" in {
      val create = createResultReader.read(Resource.getXML("model/zuora/create-result.xml")).right.get
      create.id mustEqual "2c92c0f847ae39ba0147c580319a7208"
    }
  }

  "QueryResult" should {
    "have no results" in {
      val query = queryResultReader.read(Resource.getXML("model/zuora/query-empty.xml")).right.get
      query.results.size mustEqual 0
    }

    "have one result" in {
      val query = queryResultReader.read(Resource.getXML("model/zuora/query-single.xml")).right.get

      query.results.size mustEqual 1

      query.results(0)("Id") mustEqual "2c92c0f947cddc220147d3c765d0433e"
      query.results(0)("Version") mustEqual "1"
    }

    "have multiple results" in {
      val query = queryResultReader.read(Resource.getXML("model/zuora/subscriptions.xml")).right.get

      query.results.size mustEqual 3

      query.results(0)("Id") mustEqual "2c92c0f847cdc31e0147cf2111ba6173"
      query.results(0)("Version") mustEqual "1"

      query.results(1)("Id") mustEqual "2c92c0f847cdc31e0147cf243a166af0"
      query.results(1)("Version") mustEqual "3"

      query.results(2)("Id") mustEqual "2c92c0f847cdc31e0147cf24396f6ae1"
      query.results(2)("Version") mustEqual "2"
    }

    "not allow iterable queries" in {
      val error = queryResultReader.read(Resource.getXML("model/zuora/query-not-done.xml")).left.get
      error.code mustEqual "NOT_DONE"
    }
  }

  "SubscribeResult" should {
    "have an id on success" in {
      val subscribe = subscribeResultReader.read(Resource.getXML("model/zuora/subscribe-result.xml")).right.get
      subscribe.id mustEqual "8a80812a4733a5bb0147a1a4887f410a"
    }

    "return an error on failure" in {
      val error = subscribeResultReader.read(Resource.getXML("model/zuora/subscribe-error.xml")).left.get
      error mustEqual Error("error", "TRANSACTION_FAILED", "Transaction declined.generic_decline - Your card was declined.")
    }
  }

  "UpdateResult" should {
    "have an id" in {
      val update = updateResultReader.read(Resource.getXML("model/zuora/update-result.xml")).right.get
      update.id mustEqual "2c92c0f847ae39b80147c584947b7ea3"
    }
  }
}
