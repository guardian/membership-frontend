package model

import configuration.Config.zuoraFreeEventTicketsAllowance

trait FeatureChoice {
  val zuoraCode: String
  val label: String
  val description: String
}

object FeatureChoice {
  val separator = ':'

  val all: Set[FeatureChoice] = Set(FreeEventTickets, Books)

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
  override val label = "4 books"
  override val description = """
    |Get six Guardian Live tickets to use throughout the year.
    |Use one ticket per event at the event of your choosing.
  """.stripMargin
}

case object FreeEventTickets extends FeatureChoice {
  val allowance = zuoraFreeEventTicketsAllowance
  override val zuoraCode = "Events"
  override val label = s"$allowance tickets"
  override val description = """
    |We send you 4 carefully selected Guardian published books throughout the year.
    |The exact book remains a mystery until it lands on the doorstep.
  """.stripMargin
  val unitOfMeasure = "Events"
}
