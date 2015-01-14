package model

import model.RichEvent.RichEvent

case class EventPortfolio(orderedEvents: Seq[RichEvent], normal: Seq[RichEvent], pastEvents: Option[Seq[RichEvent]]) {
  lazy val heroOpt = orderedEvents.headOption
  lazy val priority = orderedEvents.drop(1)
}
