package com.gu.memsub

sealed trait Status {
  def name: String
}
case class Legacy() extends Status {
  override val name: String = "legacy"
}
case class Current() extends Status {
  override val name: String = "current"
}
case class Upcoming() extends Status {
  override val name: String = "upcoming"
}

object Status {
  val legacy = Legacy()
  val current = Current()
  val upcoming = Upcoming()
}