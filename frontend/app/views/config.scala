package views

import com.typesafe.config.ConfigFactory

object Config {
  val config = ConfigFactory.load()

  def hideMem = {
    config.getBoolean("hideMembershipProposition")
  }
}
