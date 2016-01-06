package model

import com.github.nscala_time.time.Imports.{richDateTime, _}
import com.gu.salesforce.Tier.{Friend, Partner, Patron}
import model.Eventbrite.{EBEvent, EBResponse}
import model.EventbriteDeserializer._
import model.EventbriteTestObjects._
import org.joda.time.DateTimeZone.UTC
import org.joda.time.Instant
import org.specs2.mutable.Specification
import utils.Resource

class TicketSaleDatesTest extends Specification {

  val eventDate = new DateTime(2014, 6, 1, 18, 23)
  val testEventTimes = eventWithName().copy(created = (eventDate - 2.months).toInstant).times

  "Ticket Sales Dates" should {

    "give general availability immediately if there's very little time until the event " in {
      val saleStart = (testEventTimes.start - 2.hours).toInstant

      val datesByTier = TicketSaleDates.datesFor(testEventTimes, eventTicketClass.copy(sales_start = Some(saleStart))).datesByTier

      datesByTier(Friend) must be(saleStart)
      datesByTier(Partner) must be(saleStart)
      datesByTier(Patron) must be(saleStart)
    }

    "give patrons and partners advance tickets if there's enough lead time" in {

      val saleStart = (testEventTimes.start - 5.days).toInstant
      val datesByTier = TicketSaleDates.datesFor(testEventTimes, eventTicketClass.copy(sales_start = Some(saleStart))).datesByTier
      val priorityBookingPeriod = 48.hours.standardDuration

      datesByTier(Patron) must be_==(saleStart)
      datesByTier(Partner) must be_==(datesByTier(Partner))
      datesByTier(Friend) must be_<=(testEventTimes.start)

      (toStartOfDay(datesByTier(Patron)) to toStartOfDay(datesByTier(Friend))).duration must be >= priorityBookingPeriod

    }

    "not die if sales_start is missing" in {
      val event = Resource.getJson("model/eventbrite/event.long-lead-time.missing-sales-start.json").as[EBEvent]

      TicketSaleDates.datesFor(event.times, event.ticket_classes.head).memberAdvanceTicketSales must beSome
    }

    "be sensible given a recent snapshot of PROD Eventbrite events" in {
      val eventsWithTickets =
        Resource.getJson("model/eventbrite/owned-events.2014-10-24.PROD.page-1.json").as[EBResponse[EBEvent]].data.filter(_.ticket_classes.nonEmpty)

      forall(eventsWithTickets) { event: EBEvent =>
        val tickets = event.ticket_classes.head
        val effectiveStartDate = tickets.sales_start.getOrElse(event.created)

        val ticketSaleDates = TicketSaleDates.datesFor(event.times, tickets)
        val datesByTier = ticketSaleDates.datesByTier

        datesByTier(Patron) must be_==(effectiveStartDate)
        datesByTier(Partner) must be_==(effectiveStartDate)
        datesByTier(Friend) must be_<=(event.start)
      }
    }

  }

  private def toStartOfDay(instant: Instant) = {
    instant.toDateTime(UTC).withTimeAtStartOfDay().toInstant
  }

}
