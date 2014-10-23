package model

import com.github.nscala_time.time.Imports.{richDateTime, _}
import com.gu.membership.salesforce.Tier
import model.Eventbrite.{EBEvent, EBTickets}
import model.EventbriteDeserializer._
import org.joda.time.Instant
import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions
import utils.Resource

class TicketSaleDatesTest extends Specification with NoTimeConversions {

  val eventDate = new DateTime(2014, 6, 1, 0, 0)
  val testEvent = EventbriteTestObjects.eventWithName().copy(created = (eventDate - 1.month).toInstant)

  "Ticket Sales Dates" should {

    "all be the sale-start date if sale-start is less than 2 weeks from Event start" in {
      val saleStart = (testEvent.start - 13.days).toInstant

      val datesByTier = TicketSaleDates.datesFor(testEvent, EBTickets(sales_start = Some(saleStart)))

      datesByTier.values.toSet mustEqual Set(saleStart)
      datesByTier.keys.toSet mustEqual Set(Tier.Partner, Tier.Patron, Tier.Friend)
    }

    "give patrons and partners preferential times if sale-start is 2 or more weeks from Event start" in {
      val saleStart = testEvent.start - 2.weeks

      val datesByTier = TicketSaleDates.datesFor(testEvent, EBTickets(sales_start = Some(saleStart.toInstant)))

      datesByTier must havePairs(
        Tier.Friend -> (saleStart + 2.week).toInstant,
        Tier.Partner -> (saleStart + 1.week).toInstant,
        Tier.Patron -> (saleStart).toInstant
      )
    }

    "not die if sales_start is missing" in {
      val event = Resource.getJson("model/eventbrite/event.long-lead-time.missing-sales-start.json").as[EBEvent]
      val created = event.created.toDateTime

      TicketSaleDates.datesFor(event, event.ticket_classes.head) must havePairs(
        Tier.Friend -> (created + 2.week).toInstant,
        Tier.Partner -> (created + 1.week).toInstant,
        Tier.Patron -> created.toInstant
      )
    }
  }
}
