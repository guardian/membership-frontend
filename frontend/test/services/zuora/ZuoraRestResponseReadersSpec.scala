package services.zuora

import model.Zuora.Feature
import org.specs2.mutable.Specification

class ZuoraRestResponseReadersSpec extends Specification {
  "extractFeatures" should {

    "correctly parse subscriptions" in {

      val mockResponse = """|{
        |  "subscriptions" : [ {
        |    "ratePlans" : [ {
        |      "subscriptionProductFeatures" : [ {
        |        "id" : "2c92c0f94e4d3a3d014e5428b8210fb8",
        |        "name" : "Events",
        |        "featureCode" : "Events",
        |        "description" : ""
        |      } ]
        |    } ]
        |  } ],
        |  "success" : true
        |}
      """.stripMargin

      ZuoraRestResponseReaders.extractFeatures(mockResponse) mustEqual Seq(Feature("2c92c0f94e4d3a3d014e5428b8210fb8","Events"))
    }

    "correctly parse subscriptions with empty subscriptionProductFeatures" in {

      val mockResponse = """|{
        |  "subscriptions" : [ {
        |    "ratePlans" : [ {
        |      "subscriptionProductFeatures" : [ ]
        |    } ]
        |  } ],
        |  "success" : true
        |}
      """.stripMargin

      ZuoraRestResponseReaders.extractFeatures(mockResponse) mustEqual Seq()
    }

    "correctly parse subscriptions with no subscriptionProductFeatures" in {

      val mockResponse = """|{
        |  "subscriptions" : [ {
        |    "ratePlans" : [ {
        |    } ]
        |  } ],
        |  "success" : true
        |}
      """.stripMargin

      ZuoraRestResponseReaders.extractFeatures(mockResponse) mustEqual Seq()
    }
  }

}
