package views.support

import model.Eventbrite.EBEvent

object Event {

  val ImportantClassName = "event-trait--important"
  val ModerateClassName = "event-trait--moderate"
  val NoteWorthyClassName = "event-trait--noteworthy"

  case class Attribute(text: String, value: Boolean, className: String = "")

  case class EventAttributes(
    free: Attribute,
    noDiscount: Attribute,
    notSoldThroughEventbrite: Attribute,
    soldOut: Attribute
  )

  def eventDetail(event: EBEvent) = {
      val free = !event.isNoTicketEvent && event.generalReleaseTicket.exists(_.free)
      val noDiscount = !event.isNoTicketEvent && event.generalReleaseTicket.exists(!_.free && !event.hasMemberTicket)
      val notSoldThroughEventbrite = event.isNoTicketEvent
      val soldOut = event.isSoldOut

    EventAttributes(
      Attribute("Free Event", free, ModerateClassName),
      Attribute("No Discount", noDiscount, ImportantClassName),
      Attribute("Not Sold though Eventbrite", notSoldThroughEventbrite, NoteWorthyClassName),
      Attribute("Sold out", soldOut)
    )
  }
}
