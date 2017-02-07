package model

import com.gu.i18n.CountryGroup
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

  def promoForCountry(cg: CountryGroup) = cg match {
    case CountryGroup.Australia => ausSupporter
    case _ => supporterMinimal
  }

  def forTierAndCountry(tier: Tier, cg: CountryGroup) = (tier,cg) match {
    case (Supporter(), CountryGroup.Australia) => ausSupporter
    case (tier,CountryGroup.UK) => forTier(tier)
    case (tier,_) => forTier(tier).filterNot(b=>marketedOnlyToUK.contains(b))
  }
  val DiscountTicketTiers = Set[Tier](Staff(), Partner(), Patron())
  val PriorityBookingTiers = DiscountTicketTiers
  val ComplimentaryTicketTiers = Set[Tier](Partner(), Patron())

  val welcomePack = Benefit("welcome_pack", "Welcome pack, card and gift")
  val welcomeDog = Benefit("welcome_pack", "Welcome pack")
  val accessTicket = Benefit("access_tickets", "Access to tickets")
  val emailUpdates = Benefit("email_updates", "Regular member emails")
  var app = Benefit("app","Free access to the premium tier of the Guardian app (includes crosswords and has no adverts)")
  val priorityBooking = Benefit("priority_booking", "48hrs priority booking")
  val noBookingFees = Benefit("no_booking_fees", "No booking fees")
  val guest = Benefit("guest", "Bring a guest")
  val booksOrTickets = Benefit("books_or_tickets", s"$zuoraFreeEventTicketsAllowance tickets or 4 books", isNew = true)
  val tickets = Benefit("tickets", s"Get $zuoraFreeEventTicketsAllowance Guardian Live tickets to use throughout the year (use one ticket per event at the event of your choosing)")
  val books = Benefit("tickets", "We send you 4 carefully selected Guardian published books throughout the year (the exact book remains a mystery until it lands on the doorstep)")

  val discount = Benefit("discount", "20% discount for you and a guest")
  val uniqueExperiences = Benefit("unique_experiences", "Exclusive behind-the-scenes functions")

  val marketedOnlyToUK = Set[Benefit](accessTicket)

  val friend = Seq(
    accessTicket,
    emailUpdates
  )

  val supporter = Seq(
    welcomePack,
    app,
    accessTicket,
    emailUpdates
  )

  val ausSupporter = Seq(
    welcomeDog,
    app,
    emailUpdates
  )

  val supporterMinimal = Seq(
    welcomePack,
    app,
    emailUpdates
  )
  val partner = Seq(
    booksOrTickets,
    priorityBooking,
    noBookingFees,
    discount,
    guest,
    welcomePack,
    app,
    emailUpdates
  )
  val patron = Seq(
    tickets,
    books,
    uniqueExperiences,
    priorityBooking,
    noBookingFees,
    discount,
    guest,
    welcomePack,
    app,
    emailUpdates
  )

  val comparisonBasicList = Seq(
    welcomePack,
    app,
    accessTicket,
    emailUpdates
  )

  val staff = (partner ++ supporter).filterNot(_ == booksOrTickets).distinct
}
