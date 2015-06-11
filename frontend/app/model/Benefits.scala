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

  case class BenefitItem(identifier: String, title: String, description: String, icon: String)

  val allBenefits = Seq(
    BenefitItem("book_tickets", "Book tickets", "Book tickets to Guardian Live events", "benefit-booking"),
    BenefitItem("digital_digest", "Membership email updates", "Receive regular updates from the membership community", "benefit-digest"),
    BenefitItem("video_highlights", "Video highlights", "Watch highlights of selected Guardian Live events", "benefit-video"),
    BenefitItem("early_booking", "Early booking", "Early ticket booking on Guardian Live Events (before Friends and Supporters)", "benefit-priority-booking"),
    BenefitItem("discount", "50% off live events", "50% discount on Guardian Live events until 30 June (usually 20% discount)", "benefit-live-discount"),
    BenefitItem("discount_masterclasses", "20% off masterclasses", "20% discount on Guardian Masterclasses", "benefit-masterclasses-discount"),
    BenefitItem("membership_card", "Membership card", "Membership card and annual gift", "benefit-card"),
    BenefitItem("plus_1_guest", "+1 guest", "Bring a guest to Guardian Live with the same discount and priority booking advantages", "benefit-plus1"),
    BenefitItem("live_stream", "Live stream events", "Watch live streams of flagship Guardian Live Membership events", "benefit-stream"),
    BenefitItem("priority_booking", "Priority booking", "Additional priority ticket booking on Guardian Live Events (before Partners)", "benefit-priority-booking"),
    BenefitItem("complim_items", "Special thank-yous", "The occasional unique gift to thank you for your support", "benefit-gifts"),
    BenefitItem("unique_experiences", "Unique experiences", "Get behind the scenes of our journalism", "benefit-experiences")
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
    "digital_digest",
    "video_highlights"
  )
  val supporterBenefitsList = benefitsFilter(
    "membership_card",
    "live_stream",
    "video_highlights",
    "digital_digest",
    "book_tickets"
  )
  val partnerBenefitsList = benefitsFilter(
    "early_booking",
    "plus_1_guest",
    "live_stream",
    "discount",
    "discount_masterclasses",
    "membership_card",
    "digital_digest",
    "video_highlights",
    "book_tickets"
  )
  val patronBenefitsList = benefitsFilter(
    "discount",
    "discount_masterclasses",
    "priority_booking",
    "complim_items",
    "unique_experiences",
    "plus_1_guest",
    "membership_card",
    "live_stream",
    "digital_digest",
    "video_highlights"
  )

  val friendBenefits = Benefits("Benefits",
    friendBenefitsList,
    None,
    "Become a Friend",
    "Become a Friend to book tickets to Guardian Live events and access the Guardian members area."
  )
  val supporterBenefits = Benefits(
    "Benefits",
    supporterBenefitsList,
    Some(Pricing(50, 5, Some("1 year membership, 2 months free"))),
    "Become a Supporter",
    "Supporters keep our journalism fearless, open and free from interference."
  )
  val partnerBenefits = Benefits(
    "Supporter benefits, plus…",
    partnerBenefitsList,
    Some(Pricing(135, 15, Some("1 year membership, 3 months free"))),
    "Become a Partner",
    "Support the Guardian and experience it brought to life, with early booking and discounted tickets"
  )
  val patronBenefits = Benefits(
    "Partner benefits, plus…",
    patronBenefitsList,
    Some(Pricing(540, 60, Some("1 year membership, 3 months free"))),
    "Become a Patron",
    "Defend the Guardian’s independence and promote the open exchange of ideas, with a backstage pass to the Guardian"
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
