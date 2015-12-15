package model

import com.gu.stripe.Stripe
import com.gu.stripe.Stripe.Deserializer._
import com.gu.stripe.Stripe.{Card, Customer, StripeList}
import play.api.libs.json.Reads
import play.api.test.PlaySpecification
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
    deserialize[Stripe.Error]("error")
    deserialize[Card]("card")
    deserialize[Customer]("customer")

    deserialize[StripeList[Card]]("cardlist")
  }

}
