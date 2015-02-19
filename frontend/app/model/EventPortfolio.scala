package model

import model.RichEvent.RichEvent

// used for arbitrary groupings of events with custom titles
case class EventGroup(sequenceTitle: String, events: Seq[RichEvent])

case class EventPortfolio(
    orderedEvents: Seq[RichEvent],
    normal: Seq[RichEvent],
    pastEvents: Option[Seq[RichEvent]],
    otherEvents: Option[EventGroup]
  ) {
  lazy val heroOpt = orderedEvents.headOption
  lazy val priority = orderedEvents.drop(1)
}
