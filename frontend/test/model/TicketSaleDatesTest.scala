package model

import org.specs2.mutable.Specification
import org.specs2.time.NoTimeConversions

import com.github.nscala_time.time.Imports.{richDateTime, _}
import com.gu.membership.salesforce.Tier.{Friend, Partner, Patron}

import model.Eventbrite.{EBEvent, EBResponse, EBTicketClass}
import model.EventbriteDeserializer._
import utils.Resource
import EventbriteTestObjects._

class TicketSaleDatesTest extends Specification with NoTimeConversions {

  val eventDate = new DateTime(2014, 6, 1, 18, 23)
  val testEvent = eventWithName().copy(created = (eventDate - 2.months).toInstant)

  "Ticket Sales Dates" should {

    "give general availability immediately if there's very little time until the event " in {
      val saleStart = (testEvent.start - 2.hours).toInstant

      val datesByTier = TicketSaleDates.datesFor(testEvent, eventTicketClass.copy(sales_start = Some(saleStart))).datesByTier

      datesByTier(Friend) must be(saleStart)
      datesByTier(Partner) must be(saleStart)
      datesByTier(Patron) must be(saleStart)
    }

    "give patrons and partners advance tickets if there's enough lead time" in {
      val saleStart = (testEvent.start - 7.weeks).toInstant

      val datesByTier = TicketSaleDates.datesFor(testEvent, eventTicketClass.copy(sales_start = Some(saleStart))).datesByTier

      datesByTier(Patron) must be_==(saleStart)
      datesByTier(Patron) must be_<=(datesByTier(Partner))
      datesByTier(Partner) must be_<=(datesByTier(Friend))
      datesByTier(Friend) must be_<=(testEvent.start)
      (datesByTier(Patron) to datesByTier(Friend)).duration must be_>=(7.days.standardDuration)
    }

    "give set advance tickets to be available to start of the day if sale dates between tiers is more than a day" in {
      val saleStart = (testEvent.start - 10.days).toInstant

      val datesByTier = TicketSaleDates.datesFor(testEvent, eventTicketClass.copy(sales_start = Some(saleStart))).datesByTier

      datesByTier(Patron) must be_==(saleStart)

      val partnerTicketSale = datesByTier(Partner).toDateTime
      dateMustBeToStartOfDay(partnerTicketSale) must be_==(true)

      val friendTicketSale = datesByTier(Friend).toDateTime
      dateMustBeToStartOfDay(friendTicketSale) must be_==(true)
    }

    "give set advance tickets to be available a specific time if sale dates between tiers is less than a day" in {
      val saleStart = (testEvent.start - 36.hours).toInstant

      val datesByTier = TicketSaleDates.datesFor(testEvent, eventTicketClass.copy(sales_start = Some(saleStart))).datesByTier

      datesByTier(Patron) must be_==(saleStart)

      val partnerTicketSale = datesByTier(Partner).toDateTime
      dateMustBeToStartOfDay(partnerTicketSale) must be_==(false)

      val friendTicketSale = datesByTier(Friend).toDateTime
      dateMustBeToStartOfDay(friendTicketSale) must be_==(false)
    }

    "not die if sales_start is missing" in {
      val event = Resource.getJson("model/eventbrite/event.long-lead-time.missing-sales-start.json").as[EBEvent]
      val created = event.created.toDateTime

      TicketSaleDates.datesFor(event, event.ticket_classes.head).memberAdvanceTicketSales must beSome
    }

    "be sensible given a recent snapshot of PROD Eventbrite events" in {
      val eventsWithTickets =
        Resource.getJson("model/eventbrite/owned-events.2014-10-24.PROD.page-1.json").as[EBResponse[EBEvent]].data.filter(_.ticket_classes.nonEmpty)

      forall(eventsWithTickets) { event: EBEvent =>
        val tickets = event.ticket_classes.head
        val effectiveStartDate = tickets.sales_start.getOrElse(event.created)

        val ticketSaleDates = TicketSaleDates.datesFor(event, tickets)
        val datesByTier = ticketSaleDates.datesByTier

        datesByTier(Patron) must be_==(effectiveStartDate)
        datesByTier(Patron) must be_<=(datesByTier(Partner))
        datesByTier(Partner) must be_<=(datesByTier(Friend))
        datesByTier(Friend) must be_<=(event.start)
      }
    }
  }
  private def dateMustBeToStartOfDay(dateTime: DateTime): Boolean =
    dateTime.getHourOfDay() == 0 && dateTime.getMinuteOfHour() == 0 && dateTime.getSecondOfMinute() == 0
}
