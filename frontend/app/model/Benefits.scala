package model

import com.gu.membership.salesforce.Tier

object Benefits {

  case class Benefits(
    leadin: String,
    list: Seq[BenefitItem],
    pricing: Option[Pricing],
    cta: String,
    desc: String,
    leadBenefitsCount: Integer
  ) {
    val (leadBenefits, otherBenefits) = list.splitAt(leadBenefitsCount)
  }

  // identifier is used for CSS classes as well as within this file
  case class BenefitItem(title: String, description: String, identifier: String)

  val allBenefits = Seq(
    BenefitItem("Book tickets", "Book tickets to Guardian Live events", "book_tickets"),
    BenefitItem("Membership email updates", "Receive regular updates on the upcoming programme", "digital_digest"),
    BenefitItem("Video highlights", "Watch highlights of selected Guardian Live events", "video_highlights"),
    BenefitItem("Early booking", "Early ticket booking on Guardian Live Events (before Friends)", "early_booking"),
    BenefitItem("20% discount", "20% discount on Guardian Live tickets and selected masterclasses", "discount"),
    BenefitItem("Membership card", "", "membership_card"),
    BenefitItem("+1 guest", "Bring a guest to Guardian Live with the same discount and priority booking advantages", "plus_1_guest"),
    BenefitItem("Live stream events", "Watch live streams of Flagship events", "live_stream"),
    BenefitItem("Priority booking", "Additional priority ticket booking on Guardian Live Events (before Partners)", "priority_booking"),
    BenefitItem("20% discount", "20% discount on Guardian Live tickets", "discount_patron"),
    BenefitItem("Special thank-yous", "The occasional unique gift to thank you for your support", "complim_items"),
    BenefitItem("Unique experiences", "Get behind the scenes of our journalism", "unique_experiences")
  )

  case class Pricing(yearly: Int, monthly: Int) {
    lazy val yearlyMonthlyCost = (12 * monthly)
    lazy val yearlySaving = yearlyMonthlyCost - yearly
  }

  def benefitsFilter(identifiers: String*) = identifiers.flatMap { id =>
    allBenefits.find(_.identifier == id)
  }

  val friendsWithBenefits = benefitsFilter("book_tickets", "digital_digest", "video_highlights")

  val partnerWithBenefits = benefitsFilter("discount", "plus_1_guest", "early_booking",
    "membership_card", "live_stream", "digital_digest", "video_highlights")

  val patronWithBenefits = benefitsFilter("discount_patron", "priority_booking",
    "complim_items", "unique_experiences", "plus_1_guest", "membership_card",
    "live_stream", "digital_digest", "video_highlights")

  val friendBenefits = Benefits("As a Friend:", friendsWithBenefits,
    None, "Become a Friend", "Stay up to date and book tickets to Guardian Live events", 3)

  val partnerBenefits = Benefits("All the benefits of a Friend plus:", partnerWithBenefits,
    Some(Pricing(135, 15)), "Become a Partner", "Get closer to the stories and experience the " +
      "Guardian brought to life, with early booking and discounted tickets", 5);

  val patronBenefits = Benefits("All the benefits of a Partner plus:", patronWithBenefits,
    Some(Pricing(540, 60)), "Become a Patron", "Support the Guardian’s mission of promoting the " +
      "open exchange of ideas, with a backstage pass to the Guardian", 4)

  val details = Map(
    Tier.Friend -> friendBenefits,
    Tier.Partner -> partnerBenefits,
    Tier.Patron -> patronBenefits
  )
}
