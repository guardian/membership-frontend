package com.gu.stripe
import com.gu.stripe.Stripe._
import org.specs2.mutable.Specification
import com.gu.stripe.Stripe.Deserializer._
import org.joda.time.{Instant, LocalDate}
import utils.Resource

class StripeDeserialiserTest extends Specification {

  "Stripe deserialiser" should {
    "deserialise a charge event okay" in {
      val event = Resource.getJson("stripe/event.json").as[Event[StripeObject]]
      event.`object`.asInstanceOf[Charge].id mustEqual "chargeid"
      event.`object`.asInstanceOf[Charge].metadata("marketing-opt-in") mustEqual "true"
    }
    "deserialise a failed charge event okay" in {
      val event = Resource.getJson("stripe/failedCharge.json").as[Event[StripeObject]]
      event.`object`.asInstanceOf[Charge].id mustEqual "ch_18zUytRbpG0cjdye76ytdj"
      event.`object`.asInstanceOf[Charge].metadata("marketing-opt-in") mustEqual "false"
      event.`object`.asInstanceOf[Charge].balance_transaction must beNone
    }
    "deserialise a failed charge event okay" in {
      val error = Resource.getJson("stripe/error.json").validate[Error].get
      error mustEqual Error(
        `type` = "card_error",
        charge = Some("ch_111111111111111111111111"),
        message = Some("Your card was declined."),
        code = Some("card_declined"),
        decline_code = Some("do_not_honor"),
        param = None
      )
    }
    "deserialise a Stripe subscription (eg. guardian patrons) okay" in {
      val subscription = Resource.getJson("stripe/subscription.json").as[Subscription]
      subscription.id mustEqual "sub_1L8mv1JETvkRwpwqhowvEOlL"
      subscription.created mustEqual LocalDate.parse("2022-06-09")
    }
  }

}
