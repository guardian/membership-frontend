package views.support

import play.utils.UriEncoding.encodePathSegment

import configuration.Config
import model.Eventbrite.EBEvent

case class Social(emailSubject: String, emailMessage: String, facebookUrl: String, twitterMessage: String) {
  def encode(str: String) = encodePathSegment(str, "utf-8").replace("&", "%26")

  val encodedEmailSubject = encode(emailSubject)
  val encodedEmailMessage = encode(emailMessage)
  val encodedFacebookUrl = encode(facebookUrl)
  val encodedTwitterMessage = encode(twitterMessage)
}

object Social {
  val twitterHandle = "@" + Config.twitterUsername

  def eventDetail(event: EBEvent) = Social(
    event.name.text,
    s"The Guardian is coming to life through Guardian Live events like this one. Shall we go?\n\n${event.name.text}\n${event.memUrl}",
    event.memUrl,
    s"${event.name.text} ${event.memUrl} $twitterHandle #GuardianLive"
  )

  def eventThankyou(event: EBEvent) = Social(
    s"I'm going to ${event.name.text}!",
    s"I've just booked my ticket for ${event.name.text}. Come along too!\n\n${event.memUrl}",
    event.memUrl,
    s"I'm going to: ${event.name.text} ${event.memUrl} $twitterHandle #GuardianLive"
  )

  val joinThankyou = Social(
    "I've just joined Guardian Membership",
    s"The Guardian is coming to life through live events and meet-ups. I've joined to take part in the conversations and experiences that matter.\n\nCheck it out:\n${Config.membershipUrl}",
    Config.membershipUrl,
    s"With $twitterHandle, the Guardian is coming to life through events and meet-ups. I've just joined. Check it out. ${Config.membershipUrl}"
  )
}
