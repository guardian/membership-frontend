package services

import model.EventbriteTestObjects.TestRichEvent
import org.specs2.mutable.Specification

import model.Eventbrite.{EBEvent, EBResponse}
import model.RichEvent.{RichEvent, MasterclassEvent}
import model.EventbriteDeserializer._
import utils.Resource

class EventbriteServiceHelpersTest extends Specification {
  "availableEvents" should {
    "only show events which have tickets available for members" in {
      val response = Resource.getJson("model/eventbrite/events-with-member-tickets.json").as[EBResponse[EBEvent]]

      val events = response.data.map { event => MasterclassEvent(event, None) }

      events(0).memberTickets.map(_.id) mustEqual Seq("31250189")
      events(1).memberTickets.map(_.id) mustEqual Seq("31250231")
      events(2).memberTickets.map(_.id) mustEqual Seq()

      val availableEvents = EventbriteServiceHelpers.availableEvents(events)

      availableEvents.map(_.id) mustEqual Seq(availableEvents(0).id)
    }
  }

  "getEventsOrdering" should {
    val events = Resource.getJson("model/eventbrite/events.json").as[EBResponse[EBEvent]].data.map(TestRichEvent)

    "use only ordering if there are 4" in {
      val ordering = Seq(events(3), events(1), events(2), events(0))
      EventbriteServiceHelpers.getFeaturedEvents(ordering.map(_.id), events) mustEqual ordering
    }

    "use events list if there are no valid ordered ids" in {
      EventbriteServiceHelpers.getFeaturedEvents(Seq("1", "2"), events) mustEqual events
    }

    "pad ordering with events list if there are less than 4" in {
      val orderingIds = Seq(events(1).id, events(3).id, "1234")
      EventbriteServiceHelpers.getFeaturedEvents(orderingIds, events) mustEqual Seq(events(1), events(3), events(0), events(2))
    }
  }

}
