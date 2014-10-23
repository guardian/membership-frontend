package model

import play.api.test.PlaySpecification

import EventbriteDeserializer._
import model.Eventbrite._
import utils.Resource

class EventbriteDeserializerTest extends PlaySpecification {

  "EventbriteDeserializer" should {

    "should deserialize event json" in {
      val event = Resource.getJson("model/eventbrite/events.json")
      val ebResponse = event.as[EBResponse[EBEvent]]

      ebResponse.data.head.name.text === "Chris' big time jamboree"
    }

    "deserialize EBOrder" in {
      val order = Resource.getJson("model/eventbrite/order.json").as[EBOrder]

      order.ticketCount mustEqual 3
    }
  }

}
