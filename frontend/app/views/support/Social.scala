package views.support

import java.net.URLEncoder

import configuration.Config
import model.Eventbrite.EBEvent

case class Social(emailSubject: String, emailMessage: String, facebookUrl: String, twitterMessage: String) {
  def encode(str: String) = URLEncoder.encode(str, "UTF-8")

  val encodedFacebookUrl = encode(facebookUrl)
  val encodedTwitterMessage = encode(twitterMessage)
}

object Social {
  val twitterHandle = "@" + Config.twitterUsername

  def eventDetail(event: EBEvent) = Social(
    emailSubject=event.name.text,
    emailMessage=s"The Guardian is coming to life through Guardian Live events like this one. Shall we go?\n\n${event.name.text}\n${event.memUrl}",
    facebookUrl=event.memUrl,
    twitterMessage=s"${event.name.text} ${event.memUrl} $twitterHandle #GuardianLive"
  )

  def eventThankyou(event: EBEvent) = Social(
    emailSubject=s"I'm going to ${event.name.text}!",
    emailMessage=s"I've just booked my ticket for ${event.name.text}. Come along too!\n\n${event.memUrl}",
    facebookUrl=event.memUrl,
    twitterMessage=s"I'm going to: ${event.name.text} ${event.memUrl} $twitterHandle #GuardianLive"
  )

  val joinThankyou = Social(
    emailSubject="I'm the newest Guardian member",
    emailMessage=s"I'm the newest Guardian member ${Config.membershipUrl}${controllers.routes.Info.about()}",
    facebookUrl=Config.membershipUrl + controllers.routes.Info.about(),
    twitterMessage=s"I'm the newest Guardian member ${Config.membershipUrl}${controllers.routes.Info.about()} @gdnmembership #guardianmembership"
  )
}
