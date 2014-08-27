package model

import model.Eventbrite.EBEvent


case class EventPortfolio(priority: Seq[EBEvent], normal: Seq[EBEvent]) {
  lazy val heroOpt = priority.headOption
}
