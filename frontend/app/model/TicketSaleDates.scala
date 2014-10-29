package model

import com.github.nscala_time.time.Imports._
import com.gu.membership.salesforce.Tier
import model.Eventbrite.{EBEvent, EBTickets}
import org.joda.time.Instant
import com.gu.membership.salesforce.Tier.{Tier, Patron, Partner}

import scala.collection.immutable.SortedMap

case class TicketSaleDates(generalAvailability: Instant, memberAdvanceTicketSales: Option[Map[Tier.Tier, Instant]] = None) {
  val allDistinctDates: Set[Instant] = Set(generalAvailability) ++ memberAdvanceTicketSales.map(_.values).toSeq.flatten
  val smallestGapBetweenDates = allDistinctDates.toSeq.sorted.sliding(2).map(ds => (ds.head to ds.last).duration).toSeq.sorted.headOption
  val needToDistinguishTimes = smallestGapBetweenDates.map(_ < 24.hours).getOrElse(false)

  lazy val datesByTier = memberAdvanceTicketSales.getOrElse(Map.empty).withDefaultValue(generalAvailability)

  def tierCanBuyTicket(tier: Tier) = datesByTier(tier).isBefore(Instant.now())
}

object TicketSaleDates {

  implicit val periodOrdering = Ordering.by[Period, Duration](_.toStandardDuration)
  
  val memberLeadTimeOverGeneralRelease = SortedMap[Duration, Map[Tier.Value, Period]](
    4.hours.standardDuration -> Map(Patron -> 30.minutes, Partner -> 20.minutes),
    48.hours.standardDuration -> Map(Patron -> 4.hours, Partner -> 2.hours),
    7.days.standardDuration -> Map(Patron -> 2.days, Partner -> 1.day),
    2.weeks.standardDuration -> Map(Patron -> 5.days, Partner -> 3.days),
    6.weeks.standardDuration -> Map(Patron -> 2.weeks, Partner -> 1.week)
  )

  def datesFor(event: EBEvent, tickets: EBTickets): TicketSaleDates = {
    val effectiveSaleStart = tickets.sales_start.getOrElse(event.created)

    val saleStartLeadTimeOnEvent = (effectiveSaleStart to event.start).duration

    memberLeadTimeOverGeneralRelease.until(saleStartLeadTimeOnEvent).values.lastOption match {
      case Some(memberLeadTimes) =>
        val gapBetweenSaleStartAndGeneralRelease = memberLeadTimes.values.max

        val generalRelease = effectiveSaleStart + gapBetweenSaleStartAndGeneralRelease.standardDuration

        val memberTicketAvailabilityTimes = memberLeadTimes.mapValues(generalRelease - _.standardDuration)
        TicketSaleDates(generalRelease, Some(memberTicketAvailabilityTimes))
      case None => TicketSaleDates(effectiveSaleStart)
    }
  }
}
