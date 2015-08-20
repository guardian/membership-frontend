package model

import com.gu.membership.salesforce.Tier
import com.gu.membership.salesforce.Tier.{Partner, Patron}

object Benefits {

  val DiscountTicketTiers = Set[Tier](Partner, Patron)

  case class Benefits(
    leadin: String,
    list: Seq[BenefitItem],
    pricing: Option[Pricing],
    cta: String,
    desc: String
  )

  case class BenefitItem(identifier: String, title: String)

  case class ComparisonItem(
    description: String,
    availableSupporter: Boolean,
    availablePartner: Boolean
  )

  val allBenefits = Seq(
    BenefitItem("welcome_pack", "Welcome pack, card and gift"),
    BenefitItem("book_tickets", "Access to tickets"),
    BenefitItem("live_stream", "Access to live stream"),
    BenefitItem("email_updates", "Regular events email"),
    BenefitItem("offers_competitions", "Offers and competitions"),
    BenefitItem("priority_booking", "48hrs priority booking"),
    BenefitItem("discount", "20% discount for you and a guest"),
    BenefitItem("unique_experiences", "Exclusive behind-the-scenes functions")
  )

  case class Pricing(
    yearly: Int,
    monthly: Int,
    yearlySavingsNote: Option[String]
  ) {
    lazy val yearlyMonthlyCost = 12 * monthly
    lazy val yearlySaving = yearlyMonthlyCost - yearly
    lazy val yearlyWith6MonthSaving = yearly / 2f
    lazy val hasYearlySaving = yearlySaving > 0
  }

  private def benefitsFilter(identifiers: String*) = identifiers.flatMap { id =>
    allBenefits.find(_.identifier == id)
  }

  private def uniqueBenefits(benefits: Seq[BenefitItem], exclude: Seq[BenefitItem]) = {
    benefits.filterNot(exclude.toSet)
  }

  val friendBenefitsList = benefitsFilter(
    "book_tickets",
    "offers_competitions",
    "email_updates"
  )
  val supporterBenefitsList = benefitsFilter(
    "welcome_pack",
    "live_stream",
    "book_tickets",
    "offers_competitions",
    "email_updates"
  )
  val partnerBenefitsList = benefitsFilter(
    "priority_booking",
    "discount",
    "welcome_pack",
    "live_stream",
    "offers_competitions",
    "email_updates"
  )
  val patronBenefitsList = benefitsFilter(
    "unique_experiences",
    "priority_booking",
    "discount",
    "welcome_pack",
    "live_stream",
    "offers_competitions",
    "email_updates"
  )

  val friendBenefits = Benefits("Benefits",
    friendBenefitsList,
    None,
    "Become a Friend",
    """Receive regular updates from the membership community,
      | book tickets to Guardian Live events and access the Guardian members area""".stripMargin
  )
  val supporterBenefits = Benefits(
    "Benefits",
    supporterBenefitsList,
    Some(Pricing(50, 5, Some("1 year membership, 2 months free"))),
    "Become a Supporter",
    """Support our journalism and keep theguardian.com free of charge.
    | Get access to tickets and to the live broadcast of events""".stripMargin
  )
  val partnerBenefits = Benefits(
    "Supporter benefits, plus…",
    partnerBenefitsList,
    Some(Pricing(135, 15, Some("1 year membership, 3 months free"))),
    "Become a Partner",
    """Get priority booking and 20% discount on Guardian Live,
      | Guardian Local and most Guardian Masterclasses.""".stripMargin
  )
  val patronBenefits = Benefits(
    "Partner benefits, plus…",
    patronBenefitsList,
    Some(Pricing(540, 60, Some("1 year membership, 3 months free"))),
    "Become a Patron",
    """Show deep support for keeping the Guardian free, open and independent.
    | Get invited to a small number of exclusive, behind-the-scenes functions""".stripMargin
  )

  def details(tier: Tier) = tier match {
    case Tier.Friend => friendBenefits
    case Tier.Supporter => supporterBenefits
    case Tier.Partner => partnerBenefits
    case Tier.Patron => patronBenefits
  }

  def detailsLimited(tier: Tier) = tier match {
    case Tier.Friend => friendBenefitsList
    case Tier.Supporter => supporterBenefitsList
    case Tier.Partner => uniqueBenefits(partnerBenefitsList, supporterBenefitsList)
    case Tier.Patron => uniqueBenefits(patronBenefitsList, partnerBenefitsList)
  }

}
