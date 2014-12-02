package model

import model.Eventbrite.RichEvent

case class EventPortfolio(orderedEvents: Seq[RichEvent], normal: Seq[RichEvent]) {
  lazy val heroOpt = orderedEvents.headOption
  lazy val priority = orderedEvents.drop(1)
}
