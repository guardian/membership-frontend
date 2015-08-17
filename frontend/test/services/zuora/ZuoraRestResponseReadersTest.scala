package services.zuora

import model.Zuora.Rest
import org.joda.time.DateTime
import org.specs2.mutable.Specification
import play.api.libs.json._

class ZuoraRestResponseReadersTest extends Specification {
  import ZuoraRestResponseReaders._

  def success(v: JsObject): JsObject =
    Json.obj("success" -> true) ++ v

  def failure(errors: (String, String)*): JsObject =
    Json.obj(
      "processId" -> "process-id",
      "success" -> false,
      "reasons" -> errors.map { case (code, msg) =>
        Json.obj("code" -> code, "message" -> msg)
      }
    )

  def subscription(id: String, features: List[JsObject] = Nil): JsObject =
    Json.obj(
      "termStartDate" -> "2013-02-01",
      "termEndDate" -> "2014-02-01",
      "subscriptionNumber" -> "subscription-num",
      "status" -> "Active",
      "accountId" -> "account-id",
      "contractEffectiveDate" -> "2013-02-01",
      "initialTerm" -> 12,
      "id" -> id,
      "ratePlans" -> JsArray(List(
        Json.obj("subscriptionProductFeatures" -> features)
      ))
    )

  def feature(code: String): JsObject =
    Json.obj(
      "id" -> s"id-$code",
      "name" -> s"name for $code",
      "featureCode" -> code
    )

  "parseResponse" should {
    implicit val readUnit: Reads[Unit] = Reads.pure(())

    "parse a successful response" in {
      val jsonS = """{ "success": true }"""
      parseResponse[Unit](success(Json.obj())).isSuccess must beTrue
    }

    "parse a failure response" in {
      parseResponse[Unit](failure(("code-1", "error message"))) match {
        case Rest.Failure("process-id", List(Rest.Error("code-1", "error message"))) => ok
        case _ => ko
      }
    }

    "parse a list of subscriptions" in {
      val json = success(Json.obj("subscriptions" -> JsArray(List(subscription("id")))))

      parseResponse[List[Rest.Subscription]](json).get mustEqual List(
        Rest.Subscription("id",
                          "subscription-num",
                          "account-id",
                          DateTime.parse("2013-02-01"),
                          DateTime.parse("2014-02-01"),
                          DateTime.parse("2013-02-01"),
                          Rest.RatePlan(Nil) :: Nil,
                          Rest.Active))
    }

    "parse a list of subscriptions with product features" in {
      val json = success(Json.obj("subscriptions" -> JsArray(List(subscription("id", List(feature("events")))))))
      val subscriptions = parseResponse[List[Rest.Subscription]](json).get

      productFeatures(subscriptions) mustEqual List(
        Rest.Feature("id-events", "events")
      )
    }
  }
}
