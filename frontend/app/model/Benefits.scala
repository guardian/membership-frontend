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
    case CountryGroup.Australia => auSupporter
    case CountryGroup.UK => ukSupporter
    case CountryGroup.US => usSupporter
    case _ => supporterMinimal
  }

  def forTierAndCountry(tier: Tier, cg: CountryGroup) = (tier,cg) match {
    case (Supporter(), CountryGroup.Australia) => auSupporter
    case (tier,CountryGroup.UK) => forTier(tier)
    case (tier,_) => forTier(tier).filterNot(b=>marketedOnlyToUK.contains(b))
  }

  val DiscountTicketTiers = Set[Tier](Staff(), Partner(), Patron())
  val PriorityBookingTiers = DiscountTicketTiers
  val ComplimentaryTicketTiers = Set[Tier](Partner(), Patron())

  var adFreeApp = Benefit("ad_free_app","Free access to the premium tier of the Guardian app (includes crosswords and has no adverts)")
  val adFreeAppLocalised = Benefit("ad_free_app","An ad-free experience in our mobile app")
  val accessTicket = Benefit("access_tickets", "Access to tickets")
  val booksOrTickets = Benefit("books_or_tickets", s"$zuoraFreeEventTicketsAllowance tickets or 4 books", isNew = true)
  val booksAndTickets = Benefit("books_and_tickets", s"$zuoraFreeEventTicketsAllowance tickets and 4 books", isNew = true)
  val discount = Benefit("discount", "20% discount for you and a guest")
  val emailsUpdatesJournalists = Benefit("emails_updates_journalists", "Exclusive emails from Guardian journalists")
  val globalCommunity = Benefit("add_free_app","Joining the global Guardian Members community")
  val guest = Benefit("guest", "Bring a guest")
  val liveEvents = Benefit("live_events", "Invitations to Guardian Live events")
  val noBookingFees = Benefit("no_booking_fees", "No booking fees")
  val priorityBooking = Benefit("priority_booking", "48hrs priority booking")
  val regularEmails = Benefit("regular_emails", "Regular member emails")
  val regularEmailsAU =  Benefit("regular_emails", "Regular behind-the-scenes emails from our newsroom")
  val regularEmailsUS =  Benefit("regular_emails", "Regular behind-the-scenes emails from our newsroom")
  val regularEmailsUK =  Benefit("regular_emails", "A weekly look \"Inside the Guardian\" for our Members")
  val uniqueExperiences = Benefit("unique_experiences", "Exclusive behind-the-scenes functions")
  val welcomePack = Benefit("welcome_pack", "Welcome pack, card and gift")
  val welcomePackAU = Benefit("welcome_pack", "Welcome letter with First Dog on the Moon certificate")
  val welcomePackUK = Benefit("welcome_pack", "A welcome gift")
  val welcomePackUS = Benefit("welcome_pack", "A Guardian branded tote bag")

  val marketedOnlyToUK = Set[Benefit](accessTicket)

  val friend = Seq(
    accessTicket,
    regularEmails
  )

  val supporter = Seq(
    welcomePack,
    adFreeApp,
    accessTicket,
    regularEmails
  )

  val auSupporter = Seq(
    adFreeAppLocalised,
    regularEmailsAU,
    welcomePackAU,
    liveEvents
  )

  val usSupporter = Seq(
    adFreeAppLocalised,
    regularEmailsUS,
    welcomePackUS,
    liveEvents
  )

  val ukSupporter = Seq(
    emailsUpdatesJournalists,
    adFreeAppLocalised,
    regularEmailsUK,
    globalCommunity,
    welcomePackUK
  )

  val supporterMinimal = Seq(
    welcomePack,
    adFreeApp,
    regularEmails
  )

  val partner = Seq(
    booksOrTickets,
    priorityBooking,
    noBookingFees,
    discount,
    guest,
    welcomePack,
    adFreeApp,
    regularEmails
  )

  val patron = Seq(
    booksAndTickets,
    uniqueExperiences,
    priorityBooking,
    noBookingFees,
    discount,
    guest,
    welcomePack,
    adFreeApp,
    regularEmails
  )

  val comparisonBasicList = Seq(
    welcomePack,
    adFreeApp,
    accessTicket,
    regularEmails
  )

  val staff = (partner ++ supporter).filterNot(_ == booksOrTickets).distinct
}
