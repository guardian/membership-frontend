package model

import play.api.libs.json.{Reads, Json}
import play.api.test.PlaySpecification

import Stripe._
import StripeDeserializer._
import utils.Resource

class StripeDeserializerTest extends PlaySpecification {

  // Write my specs for me!
  def deserialize[T](name: String)(implicit reads: Reads[T]) = {
    s"deserialize $name" in {
      val resource = Resource.getJson(s"model/stripe/$name.json")
      resource.asOpt[T] must beSome
    }
  }

  "StripeDeserializer" should {
    deserialize[Error]("error")
    deserialize[Card]("card")
    deserialize[Charge]("charge")
    deserialize[Customer]("customer")
    deserialize[Plan]("plan")

    deserialize[StripeList[Card]]("cardlist")
    deserialize[StripeList[Subscription]]("subscriptionlist")
  }

}
