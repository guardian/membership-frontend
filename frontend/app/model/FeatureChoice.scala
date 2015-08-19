package model

import configuration.Config.zuoraFreeEventTicketsAllowance

trait FeatureChoice {
  val zuoraCode: String
  val label: String
}

object FeatureChoice {
  val separator = ':'

  val all: Set[FeatureChoice] = Set(
    FreeEventTickets,
    Books
  )
  val byId = all.map(fc => fc.zuoraCode -> fc).toMap
  val codes = byId.keySet

  def setToString(choices: Set[FeatureChoice]): String =
    choices.map(_.zuoraCode).mkString(separator.toString)

  def setFromString(ids: String): Set[FeatureChoice] = {
    ids.split(separator)
      .flatMap(byId.get)
      .toSet
  }
}

case object Books extends FeatureChoice {
  override val zuoraCode = "Books"
  override val label = "4 free books"
}
case object FreeEventTickets extends FeatureChoice {
  val allowance = zuoraFreeEventTicketsAllowance
  override val zuoraCode = "Events"
  override val label = s"$allowance free events"
  val uom = "Events"
}
