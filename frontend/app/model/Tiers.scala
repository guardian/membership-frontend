package model

object Tier extends Enumeration {
  type Tier = Value
  // ordering is important! ==================== //
  val None, Friend, Partner, Patron = Value

  val routeMap = Tier.values.map(t => t.toString.toLowerCase -> t).toMap
}