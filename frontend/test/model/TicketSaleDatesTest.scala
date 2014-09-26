package model

import com.gu.membership.salesforce.Tier
import model.Eventbrite.EBTickets
import org.joda.time.Instant
import org.specs2.mutable.Specification
import com.github.nscala_time.time.Imports._
import com.github.nscala_time.time.Imports.richDateTime
import org.specs2.time.NoTimeConversions

class TicketSaleDatesTest extends Specification with NoTimeConversions {

  val eventDate = new DateTime(2014, 6, 1, 0, 0)

  "Ticket Sales Dates" should {

    "all be the sale-start date if sale-start is less than 2 weeks from Event start" in {
      val saleStart = (eventDate - 13.days).toInstant
      val datesByTier = TicketSaleDates.datesFor(eventDate, EBTickets(sales_start = Some(saleStart)))

      datesByTier.values.toSet mustEqual Set(saleStart)
      datesByTier.keys.toSet mustEqual Set(Tier.Partner, Tier.Patron, Tier.Friend)
    }

    "give patrons and partners preferential times if sale-start is 2 or more weeks from Event start" in {
      val saleStart = (eventDate - 2.weeks)
      val datesByTier = TicketSaleDates.datesFor(eventDate, EBTickets(sales_start = Some(saleStart.toInstant)))

      datesByTier must havePairs(
        Tier.Friend -> (saleStart + 2.week).toInstant,
        Tier.Partner -> (saleStart + 1.week).toInstant,
        Tier.Patron -> (saleStart).toInstant
      )
    }
  }
}
