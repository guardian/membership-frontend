package model

import com.netaporter.uri.Uri
import model.RichEvent.RichEvent

sealed trait Destination

case class EventDestination(event: RichEvent, iframeUrl: Uri) extends Destination {
  val iframeHeight = {
    val ticketHeight = 60
    val iframeChrome = 560
    event.ticket_classes.length * ticketHeight + iframeChrome
  }
}

case class ContentDestination(item: MembersOnlyContent) extends Destination
