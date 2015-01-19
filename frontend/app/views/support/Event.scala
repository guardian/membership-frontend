package views.support

object Event {

  case class Detail(
    isFree: Boolean,
    isDiscounted: Boolean,
    isNoTicketEvent: Boolean,
    isSoldOut: Boolean,
    hasMemberTicket: Boolean
  )

  def eventDetail(event: model.RichEvent.RichEvent) = {

      val isFree = !event.isNoTicketEvent && event.generalReleaseTicket.exists(_.free)
      val isDiscounted = !event.isNoTicketEvent && event.generalReleaseTicket.exists(!_.free && !event.hasMemberTicket)
      val isNoTicketEvent = event.isNoTicketEvent
      val isSoldOut = event.isSoldOut
      val hasMemberTicket = event.hasMemberTicket

      Detail(isFree, isDiscounted, isNoTicketEvent, isSoldOut, hasMemberTicket)
  }
}
