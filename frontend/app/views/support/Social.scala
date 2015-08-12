package views.support

import java.net.URLEncoder

import configuration.{Config, Social => SocialConfig}
import model.RichEvent.RichEvent

case class Social(emailSubject: String, emailMessage: String, facebookUrl: String, twitterMessage: String) {
  def encode(str: String) = URLEncoder.encode(str, "UTF-8")
  val encodedFacebookUrl = encode(facebookUrl)
  val encodedTwitterMessage = encode(twitterMessage)
}

object Social {

  def eventDetail(event: RichEvent) = Social(
    emailSubject=event.name.text,
    emailMessage=s"The Guardian is coming to life through Guardian Live events like this one. Shall we go?\n\n${event.name.text}\n${event.memUrl}",
    facebookUrl=event.memUrl,
    twitterMessage=s"${event.name.text} ${event.memUrl} ${event.metadata.socialHashtag.mkString}"
  )

  def eventThankyou(event: RichEvent) = Social(
    emailSubject=s"I'm going to ${event.name.text}!",
    emailMessage=s"I've just booked my ticket for ${event.name.text}. Come along too!\n\n${event.memUrl}",
    facebookUrl=event.memUrl,
    twitterMessage=s"I'm going to: ${event.name.text} ${event.memUrl} ${event.metadata.socialHashtag.mkString}"
  )

  val joinThankyou = Social(
    emailSubject="I'm the newest Guardian member",
    emailMessage=s"I'm the newest Guardian member ${Config.membershipUrl}",
    facebookUrl=Config.membershipUrl,
    twitterMessage=s"I'm the newest Guardian member ${Config.membershipUrl} ${SocialConfig.twitterHashtag}"
  )
}
