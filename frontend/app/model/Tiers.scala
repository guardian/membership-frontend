package model

object Tier extends Enumeration {
  type Tier = Value
  // ordering is important! ==================== //
  val RegisteredUser, AnonymousUser, Friend, Partner, Patron = Value

}