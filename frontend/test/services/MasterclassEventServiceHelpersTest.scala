package services

import org.specs2.mutable.Specification

import model.Eventbrite.{MasterclassEvent, EBEvent, EBResponse}
import model.EventbriteDeserializer._
import utils.Resource

class MasterclassEventServiceHelpersTest extends Specification {
  "availableEvents" should {
    "only show events which have tickets available for members" in {
      val response = Resource.getJson("model/eventbrite/events-with-member-tickets.json").as[EBResponse[EBEvent]]

      val events = response.data.map { event => MasterclassEvent(event, None) }

      events(0).memberTickets.map(_.id) mustEqual Seq("31250189")
      events(1).memberTickets.map(_.id) mustEqual Seq("31250231")
      events(2).memberTickets.map(_.id) mustEqual Seq()

      val availableEvents = MasterclassEventServiceHelpers.availableEvents(events)

      availableEvents.map(_.id) mustEqual Seq(events(0).id)
    }
  }

}
