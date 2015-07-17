package model

import com.github.nscala_time.time.Imports._
import com.gu.membership.salesforce.Tier
import com.gu.membership.salesforce.Tier.{Partner, Patron}
import model.Eventbrite.{EBTicketClass, EventTimes}
import org.joda.time.DateTimeZone.UTC
import org.joda.time.Instant

import scala.collection.immutable.SortedMap

case class TicketSaleDates(generalAvailability: Instant, memberAdvanceTicketSales: Option[Map[Tier, Instant]] = None, needToDistinguishTimes: Boolean = false) {

  lazy val datesByTier = memberAdvanceTicketSales.getOrElse(Map.empty).withDefaultValue(generalAvailability)

  def tierCanBuyTicket(tier: Tier) = datesByTier(tier).isBefore(Instant.now())
  def anyoneCanBuyTicket = generalAvailability.isBefore(Instant.now())
}

object TicketSaleDates {

  implicit val periodOrdering = Ordering.by[Period, Duration](_.toStandardDuration)

  /**
   * All tiers with event benefits get a 48 hour lead-time
   * aside from cases where an event has been released with < 48 hours notice
   */
  val memberLeadTimeOverGeneralRelease = SortedMap[Duration, Map[Tier, Period]](
    4.hours.standardDuration -> Map(Patron -> 30.minutes, Partner -> 30.minutes),
    48.hours.standardDuration -> Map(Patron -> 4.hours, Partner -> 4.hours),
    7.days.standardDuration -> Map(Patron -> 48.hours, Partner -> 48.hours)
  )

  def datesFor(eventTimes: EventTimes, tickets: EBTicketClass): TicketSaleDates = {
    val effectiveSaleStart = tickets.sales_start.getOrElse(eventTimes.created)

    val saleStartLeadTimeOnEvent = (effectiveSaleStart to eventTimes.start).duration

    memberLeadTimeOverGeneralRelease.until(saleStartLeadTimeOnEvent).values.lastOption match {
      case Some(memberLeadTimes) =>
        val gapBetweenSaleStartAndGeneralRelease = memberLeadTimes.values.max

        val generalRelease = effectiveSaleStart + gapBetweenSaleStartAndGeneralRelease.standardDuration

        val memberTicketAvailabilityTimes = memberLeadTimes.mapValues(generalRelease - _.standardDuration)

        val needToDistinguishTicketTimes = needToDistinguishTimes(generalRelease, Some(memberTicketAvailabilityTimes))
        if(!needToDistinguishTicketTimes) {

          val memberAdvanceTicketSales = memberTicketAvailabilityTimes.mapValues(maxStartSaleTime(effectiveSaleStart, _))

          TicketSaleDates(toStartOfDay(generalRelease), Some(memberAdvanceTicketSales), needToDistinguishTicketTimes)

        }
        else TicketSaleDates(generalRelease, Some(memberTicketAvailabilityTimes), needToDistinguishTicketTimes)

      case None => TicketSaleDates(effectiveSaleStart)
    }
  }


  private def maxStartSaleTime(effectiveSaleStart: Instant, tierSaleDate:Instant) = {
    Seq(effectiveSaleStart, toStartOfDay(tierSaleDate)).max
  }

  private def toStartOfDay(instant: Instant) = instant.toDateTime(UTC).withTimeAtStartOfDay().toInstant

  private def needToDistinguishTimes(generalAvailability: Instant, memberAdvanceTicketSales: Option[Map[Tier, Instant]] = None) = {
    val allDistinctDates: Set[Instant] = Set(generalAvailability) ++ memberAdvanceTicketSales.map(_.values).toSeq.flatten
    val smallestGapBetweenDates = allDistinctDates.toSeq.sorted.sliding(2).map(ds => (ds.head to ds.last).duration).toSeq.sorted.headOption

    smallestGapBetweenDates.exists(_ < 24.hours)
  }
}
