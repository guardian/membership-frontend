package model

import model.RichEvent.RichEvent
import org.joda.time.LocalDate

import scala.collection.immutable.SortedMap

// used for arbitrary groupings of events with custom titles
case class EventGroup(sequenceTitle: String, events: Seq[RichEvent])

case class EventPortfolio(
    orderedEvents: Seq[RichEvent],
    normal: Seq[RichEvent],
    pastEvents: Seq[RichEvent],
    otherEvents: Option[EventGroup]
  ) {
  lazy val heroOpt = orderedEvents.headOption
  lazy val priority = orderedEvents.drop(1)
}

case class EventCollections(
    trending: Seq[RichEvent],
    topSelling: Seq[RichEvent],
    thisWeek: Seq[RichEvent],
    nextWeek: Seq[RichEvent],
    recentlyCreated: Seq[RichEvent],
    partnersOnly: Seq[RichEvent],
    programmingPartnerEvents: Option[EventGroup]
)

case class EventBrandCollection(
  live: Seq[RichEvent],
  local: Seq[RichEvent],
  masterclasses: Seq[RichEvent]
)

case class CalendarMonthDayGroup(
  list: SortedMap[LocalDate, SortedMap[LocalDate, Seq[RichEvent]]]
)
