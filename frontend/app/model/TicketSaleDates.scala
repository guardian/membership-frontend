package model

import com.github.nscala_time.time.Imports._
import com.gu.membership.salesforce.Tier
import model.Eventbrite.{EBEvent, EBTickets}
import org.joda.time.Instant

object TicketSaleDates {
  def datesFor(event: EBEvent, tickets: EBTickets): Map[Tier.Value, Instant] = {
    val effectiveSaleStart = tickets.sales_start.getOrElse(event.created)

    if (effectiveSaleStart > event.start - 2.weeks) {
      Map(Tier.Patron -> effectiveSaleStart, Tier.Partner -> effectiveSaleStart, Tier.Friend -> effectiveSaleStart)
    } else {
      val saleStartDateTime = effectiveSaleStart.toDateTime()
      Map(Tier.Patron -> effectiveSaleStart, Tier.Partner -> (saleStartDateTime + 1.week).toInstant, Tier.Friend -> (saleStartDateTime + 2.weeks).toInstant)
    }
  }
}
