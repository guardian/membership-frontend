package model

import com.gu.membership.salesforce.Tier
import com.gu.membership.salesforce.Tier.{Partner, Patron, Staff}
import configuration.Config.zuoraFreeEventTicketsAllowance

object Benefits {

  val DiscountTicketTiers = Set[Tier](Staff, Partner, Patron)
  val ComplimenataryTicketTiers = Set[Tier](Partner, Patron)

  case class Pricing(
    yearly: Int,
    monthly: Int,
    yearlySavingsNote: Option[String],
    yearlySavingsOverride: Option[String] = None,
    hideYearlySaving: Boolean = false
  ) {
    val hasOverride = yearlySavingsOverride.isDefined

    lazy val yearlyMonthlyCost = 12 * monthly
    lazy val yearlySaving = yearlyMonthlyCost - yearly
    lazy val yearlyWith6MonthSaving = yearly / 2f
    lazy val hasYearlySaving = yearlySaving > 0
  }

  case class BenefitItem(identifier: String, title: String, isNew: Boolean = false)
  case class BenefitHighlight(description: String, isNew: Boolean = false)

  val allBenefits = Seq(
    BenefitItem("welcome_pack", "Welcome pack, card and gift"),
    BenefitItem("access_tickets", "Access to tickets"),
    BenefitItem("live_stream", "Access to live stream"),
    BenefitItem("email_updates", "Regular events email"),
    BenefitItem("offers_competitions", "Offers and competitions"),
    BenefitItem("priority_booking", "48hrs priority booking"),
    BenefitItem("no_booking_fees", "No booking fees"),
    BenefitItem("books_or_tickets", s"$zuoraFreeEventTicketsAllowance tickets or 4 books", isNew = true),
    BenefitItem("books_and_tickets", s"Both $zuoraFreeEventTicketsAllowance tickets and 4 books", isNew = true),
    BenefitItem("discount", "20% discount"),
    BenefitItem("guest", "Bring a guest"),
    BenefitItem("unique_experiences", "Exclusive behind-the-scenes functions")
  )

  case class Benefits(
    tier: Tier,
    leadin: String,
    highlights: Seq[BenefitHighlight],
    altSavingMessage : Option[String] = None
  ) {
    val pricing = tier match {
      case Tier.Friend => None
      case Tier.Supporter => Option(Pricing(50, 5, Option("1 year membership, 2 months free")))
      case Tier.Partner => Option(Pricing(135, 15, Option("1 year membership, 3 months free (ends 31 Oct)"), Some("3 months free")))
      case Tier.Patron => Option(Pricing(540, 60, Option("1 year membership, 3 months free  (ends 31 Oct)"), Some("3 months free")))
      case _ => None
    }
    val list = tier match {
      case Tier.Friend => friendBenefitsList
      case Tier.Supporter => supporterBenefitsList
      case Tier.Partner => partnerBenefitsList
      case Tier.Patron => patronBenefitsList
      case _ => Seq.empty
    }
    val cta = s"Become a ${tier.name.toLowerCase}"
  }

  private def benefitsFilter(identifiers: String*) = identifiers.flatMap { id =>
    allBenefits.find(_.identifier == id)
  }

  private def uniqueBenefits(benefits: Seq[BenefitItem], exclude: Seq[BenefitItem]) = {
    benefits.filterNot(exclude.toSet)
  }

  val friendBenefitsList = benefitsFilter(
    "access_tickets",
    "offers_competitions",
    "email_updates"
  )
  val supporterBenefitsList = benefitsFilter(
    "welcome_pack",
    "live_stream",
    "access_tickets",
    "offers_competitions",
    "email_updates"
  )
  val partnerBenefitsList = benefitsFilter(
    "books_or_tickets",
    "priority_booking",
    "no_booking_fees",
    "discount",
    "guest",
    "welcome_pack",
    "live_stream",
    "offers_competitions",
    "email_updates"
  )
  val patronBenefitsList = benefitsFilter(
    "books_and_tickets",
    "unique_experiences",
    "priority_booking",
    "no_booking_fees",
    "discount",
    "guest",
    "welcome_pack",
    "live_stream",
    "offers_competitions",
    "email_updates"
  )

  def details(tier: Tier) = tier match {
    case Tier.Friend => Benefits(Tier.Friend, "Benefits", List(
      BenefitHighlight("""Receive regular updates from the membership community,
        | book tickets to Guardian Live events and access the Guardian members area""".stripMargin)
    ))
    case Tier.Supporter => Benefits(Tier.Supporter, "Benefits", List(
      BenefitHighlight("Support our journalism and keep theguardian.com free of charge"),
      BenefitHighlight("Get access to tickets and to the live broadcast of events")
    ))
    case Tier.Partner => Benefits(Tier.Partner, "Supporter benefits, plus…", List(
      BenefitHighlight("Get priority booking and 20% discount on Guardian Live, Guardian Local and most Guardian Masterclasses"),
      BenefitHighlight(s"Includes $zuoraFreeEventTicketsAllowance tickets to Guardian Live events (or 4 Guardian-published books) per year", isNew = true)
    ))
    case Tier.Patron => Benefits(Tier.Patron, "Partner benefits, plus…", List(
      BenefitHighlight("Show deep support for keeping the Guardian free, open and independent."),
      BenefitHighlight("Get invited to a small number of exclusive, behind-the-scenes functions")
    ))
    case Tier.Staff => Benefits(Tier.Staff, "Staff benefits", Nil)
  }

  def detailsLimited(tier: Tier) = tier match {
    case Tier.Friend => friendBenefitsList
    case Tier.Supporter => supporterBenefitsList
    case Tier.Partner => uniqueBenefits(partnerBenefitsList, supporterBenefitsList)
    case Tier.Patron => uniqueBenefits(patronBenefitsList, partnerBenefitsList)
    case Tier.Staff => uniqueBenefits(partnerBenefitsList, supporterBenefitsList).filterNot(_.identifier == "books_or_tickets")
  }

}
