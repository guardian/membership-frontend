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

    "should deserialize event location" in {
      val event = Resource.getJson("model/eventbrite/owned-events.2014-10-24.PROD.page-1.json")
      val ebResponse = event.as[EBResponse[EBEvent]]

      ebResponse.data.head.venue.address.flatMap(_.address_2) mustEqual(Some("90 York Way"))
    }

    "deserialize EBOrder" in {
      val order = Resource.getJson("model/eventbrite/order.json").as[EBOrder]

      order.ticketCount mustEqual 3
    }

    "deserialize a really complicated ticket class structure" in {
      val event = Resource.getJson("model/eventbrite/event-ticket-classes.json").as[EBEvent]
      event.ticket_classes.length mustEqual 5
    }
  }

}
