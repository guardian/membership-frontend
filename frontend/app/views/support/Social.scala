package views.support

import configuration.Config

case class Social(emailSubject: String, emailMessage: String, twitterMessage: String)

object Social {
  val twitterHandle = "@" + Config.twitterUsername

  val thankyou = Social("Great event", "We are changing the world with our messages", s"Please come $twitterHandle")
}
