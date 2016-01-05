package model

import com.gu.salesforce.Tier
import com.gu.salesforce.Tier._
import configuration.Config.zuoraFreeEventTicketsAllowance


object Benefits {
  def forTier(tier: Tier) = tier match {
    case Staff() => Benefits.staff
    case Friend() => Benefits.friend
    case Supporter() => Benefits.supporter
    case Partner() => Benefits.partner
    case Patron() => Benefits.patron
  }

  val DiscountTicketTiers = Set[Tier](Staff(), Partner(), Patron())
  val PriorityBookingTiers = DiscountTicketTiers
  val ComplimenataryTicketTiers = Set[Tier](Partner(), Patron())

  val welcomePack = Benefit("welcome_pack", "Welcome pack, card and gift")
  val accessTicket = Benefit("access_tickets", "Access to tickets")
  val liveStream = Benefit("live_stream", "Access to live stream")
  val emailUpdates = Benefit("email_updates", "Regular member emails")
  val offersCompetitions = Benefit("offers_competitions", "Offers and competitions")
  val priorityBooking = Benefit("priority_booking", "48hrs priority booking")
  val noBookingFees = Benefit("no_booking_fees", "No booking fees")
  val guest = Benefit("guest", "Bring a guest")
  val booksOrTickets = Benefit("books_or_tickets", s"$zuoraFreeEventTicketsAllowance tickets or 4 books", isNew = true)
  val booksAndTickets = Benefit("books_and_tickets", s"$zuoraFreeEventTicketsAllowance tickets and 4 books", isNew = true)

  val discount = Benefit("discount", "20% discount for you and a guest")
  val uniqueExperiences = Benefit("unique_experiences", "Exclusive behind-the-scenes functions")

  val marketedOnlyToUK = Set[Benefit](liveStream, accessTicket, offersCompetitions)

  val friend = Seq(
    accessTicket,
    offersCompetitions,
    emailUpdates
  )

  val supporter = Seq(
    welcomePack,
    liveStream,
    accessTicket,
    offersCompetitions,
    emailUpdates
  )
  val partner = Seq(
    booksOrTickets,
    priorityBooking,
    noBookingFees,
    discount,
    guest,
    welcomePack,
    liveStream,
    offersCompetitions,
    emailUpdates
  )
  val patron = Seq(
    booksAndTickets,
    uniqueExperiences,
    priorityBooking,
    noBookingFees,
    discount,
    guest,
    welcomePack,
    liveStream,
    offersCompetitions,
    emailUpdates
  )

  val comparisonBasicList = Seq(
    welcomePack,
    liveStream,
    accessTicket,
    offersCompetitions,
    emailUpdates
  )

  val staff = (partner ++ supporter).filterNot(_ == booksOrTickets).distinct

  val comparisonHiglightsList = Seq(
    booksOrTickets,
    priorityBooking,
    discount,
    uniqueExperiences
  )
}
