package views.support

import model.EventbriteDeserializer
import org.specs2.mutable.Specification
import model.Eventbrite._
import utils.Resource
import EventbriteDeserializer._

class EventTest extends Specification {

  "eventDetail" should {
    "report event as a no discount event" in {
      val event = Resource.getJson("model/eventbrite/event-standard-ticket-classes.json").as[EBEvent]
      val eventDetail = Event.eventDetail(event)

      eventDetail.noDiscount.value must beTrue
      eventDetail.free.value must beFalse
      eventDetail.notSoldThroughEventbrite.value must beFalse
      eventDetail.soldOut.value must beFalse
    }
    "report event as a free event" in {
      val event = Resource.getJson("model/eventbrite/event-free-ticket-classes.json").as[EBEvent]
      val eventDetail = Event.eventDetail(event)

      eventDetail.free.value must beTrue
      eventDetail.noDiscount.value must beFalse
      eventDetail.notSoldThroughEventbrite.value must beFalse
      eventDetail.soldOut.value must beFalse
    }
    "report event as not being sold through Eventbrite event" in {
      val event = Resource.getJson("model/eventbrite/event-not-sold-through-eventbrite-ticket-classes.json").as[EBEvent]
      val eventDetail = Event.eventDetail(event)

      eventDetail.notSoldThroughEventbrite.value must beTrue
      eventDetail.noDiscount.value must beFalse
      eventDetail.free.value must beFalse
      eventDetail.soldOut.value must beFalse
    }
    "report event as sold out event" in {
      val event = Resource.getJson("model/eventbrite/event-sold-out-ticket-classes.json").as[EBEvent]
      val eventDetail = Event.eventDetail(event)

      eventDetail.soldOut.value must beTrue
      eventDetail.noDiscount.value must beFalse
      eventDetail.free.value must beFalse
      eventDetail.notSoldThroughEventbrite.value must beFalse
    }
  }
}

