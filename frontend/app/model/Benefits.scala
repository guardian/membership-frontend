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
  val liveStream = Benefit("live_stream", "Access to live stream")
  val emailUpdates = Benefit("email_updates", "Regular member emails")
  var app = Benefit("app","Free access to the premium tier of the Guardian app (includes crosswords and has no adverts)")
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
    app,
    accessTicket,
    offersCompetitions,
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
    liveStream,
    app,
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
    app,
    offersCompetitions,
    emailUpdates
  )

  val comparisonBasicList = Seq(
    welcomePack,
    liveStream,
    app,
    accessTicket,
    offersCompetitions,
    emailUpdates
  )

  val staff = (partner ++ supporter).filterNot(_ == booksOrTickets).distinct
}
