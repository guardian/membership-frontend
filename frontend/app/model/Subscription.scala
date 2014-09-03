package model

object Subscription {
  case class Subscription(id: String)

  case class Amendment(ids: Seq[String])
}
