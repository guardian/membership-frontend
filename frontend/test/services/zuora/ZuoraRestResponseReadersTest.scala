package services.zuora

import model.Zuora.Feature
import org.specs2.mutable.Specification

class ZuoraRestResponseReadersTest extends Specification {
  "parseResponde" should {
    "return a RestSuccess object when the response is successful" in {
      """
        |{
        |  "success": true,
        |  "nextPage": "https://api.zuora.com/rest/v1/subscriptions/accounts/A00001115?page=2&pageSize=10",
        |  "subscriptions": [
        |    {
        |      "termStartDate": "2013-02-01",
        |      "subscriptionNumber": "A-S00001081",
        |      "accountNumber": "A00001115",
        |      "totalContractedValue": 400.0,
        |      "accountName": "subscribeCallYan_1",
        |      "ratePlans": [
        |        {
        |          "ratePlanName": "QSF_Tier",
        |          "productName": "Recurring Charge",
        |          "ratePlanCharges": [
        |            {
        |              "name": "TieredPrice",
        |              "listPriceBase": "Per_Month",
        |              "billingPeriod": "Quarter",
        |              "model": "Tiered",
        |              "type": "Recurring",
        |              "pricingSummary": "0 to 10 ONE_DOWN: USD10/ONE_DOWN;  10.1 to 20 ONE_DOWN: USD20/ONE_DOWN;  20.1 to 30 ONE_DOWN: USD30/ONE_DOWN;  30.1 to 40 ONE_DOWN: USD40/ONE_DOWN;  40.1 ONE_DOWN or more: USD50/ONE_DOWN",
        |              "quantity": 10.0,
        |              "billingDay": "DefaultFromCustomer",
        |              "id": "2c92c8f83dc4f752013dc72c24d9016c"
        |            },
        |            {
        |              "name": "TieredPrice Prepayment Charge",
        |              "billingPeriod": null,
        |              "model": "FlatFee",
        |              "type": "OneTime",
        |              "pricingSummary": "USD200",
        |              "quantity": null,
        |              "billingDay": "DefaultFromCustomer",
        |              "id": "2c92c8f83dc4f752013dc72c252f0171"
        |            }
        |          ],
        |          "id": "subscription-id"
        |        }
        |      ],
        |      "status": "Active",
        |      "renewalTerm": 3,
        |      "accountId": "2c92a0f9391832b10139183e277a0042",
        |      "autoRenew": true,
        |      "termType": "TERMED",
        |      "termEndDate": "2014-02-01",
        |      "contractEffectiveDate": "2013-02-01",
        |      "notes": "Test POST subscription from z-ruby-sdk",
        |      "initialTerm": 12,
        |      "subscriptionStartDate": "2013-02-01",
        |      "id": "2c92c8f83dc4f752013dc72c24ee016d"
        |    },
        |  ]
        |}
        |
      """.stripMargin

    }

  }
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
