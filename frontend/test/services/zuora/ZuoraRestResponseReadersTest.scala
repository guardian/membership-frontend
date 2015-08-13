package services.zuora

import model.Zuora.{RatePlan, Feature, Rest}
import net.liftweb.json.JsonAST.{JObject, JValue}
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

      parseResponse[List[Rest.Subscription]](json) match {
        case Rest.Success(Rest.Subscription("id",
                                            "subscription-num",
                                            "account-id",
                                            _:DateTime,
                                            _:DateTime,
                                            _:DateTime,
                                            Rest.RatePlan(Nil) :: Nil,
                                            Rest.Active) :: Nil) => ok
        case other =>
          ko
      }
    }

    "parse a list of subscription with product features" in {
      val json = success(Json.obj("subscriptions" -> JsArray(List(subscription("id", List(feature("events")))))))

      parseResponse[List[Rest.Subscription]](json) match {
        case Rest.Success(List(Rest.Subscription(_, _, _, _, _, _, List(Rest.RatePlan(List(Rest.Feature(_, "events")))), _))) =>
          ok
        case _ =>
          ko
      }
    }
  }
}
