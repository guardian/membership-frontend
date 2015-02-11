package services

import model.Eventbrite.{EBEvent, EBResponse}
import model.EventbriteDeserializer._
import model.RichEvent.MasterclassEvent
import org.specs2.mutable.Specification
import services.MasterclassEventsProvider.MasterclassesWithAvailableMemberDiscounts
import utils.Resource

class MasterclassEventsProviderTest extends Specification {

  "service" should {
    "only show masterclasses which have discount tickets available for members" in {
      val response = Resource.getJson("model/eventbrite/events-with-member-tickets.json").as[EBResponse[EBEvent]]

      val events = response.data.map { event => MasterclassEvent(event, None) }

      val availableEvents = events.filter(MasterclassesWithAvailableMemberDiscounts)

      availableEvents.map(_.id) mustEqual Seq("13675207915")
    }
  }

}
