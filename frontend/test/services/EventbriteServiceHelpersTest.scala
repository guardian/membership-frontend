package services

import model.Eventbrite.{EBEvent, EBResponse}
import model.EventbriteDeserializer._
import model.EventbriteTestObjects.TestRichEvent
import org.specs2.mutable.Specification
import utils.Resource
import utils.Implicits._

class EventbriteServiceHelpersTest extends Specification {

  "getEventsOrdering" should {
    val events = Resource.getJson("model/eventbrite/owned-events.2014-10-24.PROD.page-1.json").as[EBResponse[EBEvent]].data.map(eD => TestRichEvent(eD.toAssumedEventWithDescription))

    "use only ordering if there are 4" in {
      val ordering = Seq(events(3), events(1), events(2), events(0))
      EventbriteServiceHelpers.getFeaturedEvents(ordering.map(_.underlying.ebEvent.id), events) mustEqual ordering
    }

    "use regular, non sold-out events, if there are no valid event ids among the ordered ids" in {
      EventbriteServiceHelpers.getFeaturedEvents(Seq("666", "668"), events).map(_.underlying.ebEvent.id) mustEqual Seq("11871378613", "12017270981", "12016091453", "12104018445")
    }

    "pad ordering with events list if there are less than 4" in {
      val orderingIds = Seq(events(1).underlying.ebEvent.id, events(3).underlying.ebEvent.id, "666")
      EventbriteServiceHelpers.getFeaturedEvents(orderingIds, events).map(_.underlying.ebEvent.id) mustEqual Seq(events(1).underlying.ebEvent.id, events(3).underlying.ebEvent.id, events(0).underlying.ebEvent.id, events(2).underlying.ebEvent.id)
    }
  }

}
