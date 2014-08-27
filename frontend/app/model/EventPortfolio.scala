package model

import model.Eventbrite.EBEvent


case class EventPortfolio(orderedEvents: Seq[EBEvent], normal: Seq[EBEvent]) {
  lazy val heroOpt = orderedEvents.headOption
  lazy val priority = orderedEvents.tail
}
