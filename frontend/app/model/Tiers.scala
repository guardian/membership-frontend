package model

object Tier extends Enumeration {
  type Tier = Value
  val RegisteredUser, AnonymousUser, Friend, Partner, Patron = Value
}