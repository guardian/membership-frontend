package model

import com.github.nscala_time.time.Imports._
import com.gu.membership.salesforce.Tier
import model.Eventbrite.EBTickets
import org.joda.time.Instant

object TicketSaleDates {
  def datesFor(eventStart: DateTime, tickets: EBTickets): Map[Tier.Value, Instant] = {
    val saleStart = tickets.sales_start.get

    if (saleStart > eventStart - 2.weeks) {
      Map(Tier.Patron -> saleStart, Tier.Partner -> saleStart, Tier.Friend -> saleStart)
    } else {
      val saleStartDateTime = saleStart.toDateTime(eventStart.getChronology)
      Map(Tier.Patron -> saleStart, Tier.Partner -> (saleStartDateTime + 1.week).toInstant, Tier.Friend -> (saleStartDateTime + 2.weeks).toInstant)
    }
  }
}
