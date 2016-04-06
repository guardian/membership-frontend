package model

import com.gu.salesforce.Tier
import com.gu.salesforce.Tier._
import views.support.DisplayText.Highlight
import configuration.Config.zuoraFreeEventTicketsAllowance
import scalaz.NonEmptyList

object Highlights {
  def forTier(tier: Tier) = tier match {
    case Friend() => friend
    case Supporter() => supporter
    case Partner() => partner
    case Patron() => patron
    case Staff() => Nil
  }

  val updatesAndTickets = Highlight(
      """Receive regular updates from the membership community,
        | book tickets to Guardian Live and Local events and access the Guardian members area""".stripMargin)

  val support = Highlight(
      "Support the independence of the Guardian and our award-winning journalism")
  val tickets = Highlight(
      "Get access to tickets and to the live broadcast of events")
  val freeTickets = Highlight(
      s"""Includes $zuoraFreeEventTicketsAllowance tickets to Guardian Live events
         | (or 4 Guardian-published books) per year""".stripMargin)
  val priorityBooking = Highlight(
      "Get priority booking and 20% discount on Guardian Live, Guardian Local and most Guardian Masterclasses")
  val deepSupport = Highlight(
      "Show deep support for keeping the Guardian open and independent")
  val behindTheScenes = Highlight(
      "Get invited to a small number of exclusive, behind-the-scenes functions")

  val friend = Seq(updatesAndTickets)
  val supporter = Seq(support, tickets)
  val partner = Seq(freeTickets, priorityBooking)
  val patron = Seq(deepSupport, behindTheScenes)

  val marketedOnlyToUK = Set[Highlight](tickets, freeTickets, priorityBooking)

  val ehLandingPage = NonEmptyList(
      Highlight(
          "Support Guardian journalism and our coverage of critical, under-reported stories from around the world"),
      Highlight(
          "Enjoy a host of benefits, from Guardian Live tickets to the best Guardian books"),
      Highlight(
          "Receive a free English Heritage membership worth £88 when you become a Guardian Partner by 31 March")
  )

  val discountLandingPage = NonEmptyList(
      Highlight(
          "Support Guardian journalism and our coverage of critical, under-reported stories from around the world"),
      Highlight(
          "Enjoy a host of benefits, from Guardian Live tickets to the best Guardian books"),
      Highlight("Enjoy £50 off  your first year")
  )
  val discountLandingPageLong = NonEmptyList(
      Highlight(
          "Your choice of 6 tickets to Guardian Live events or 4 Guardian-published books per year"),
      Highlight(" Priority booking for Guardian Live and Guardian Local"),
      Highlight(" No booking fees"),
      Highlight(
          "A 20% discount on Guardian Local, most Guardian Masterclasses and any further Guardian Live tickets"),
      Highlight(
          "  Bring a guest with the same discount to Guardian Live and Local events"),
      Highlight("  Welcome pack and gift")
  )
}
