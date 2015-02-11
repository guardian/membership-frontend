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

  case class BenefitItem(title: String, description: String, identifier: String, icon: String)

  val allBenefits = Seq(
    BenefitItem("Book tickets", "Book tickets to Guardian Live events", "book_tickets", "benefit-booking"),
    BenefitItem("Membership email updates", "Receive regular updates on the upcoming programme", "digital_digest", "benefit-digest"),
    BenefitItem("Video highlights", "Watch highlights of selected Guardian Live events", "video_highlights", "benefit-video"),
    BenefitItem("Early booking", "Early ticket booking on Guardian Live Events (before Friends)", "early_booking", "benefit-priority-booking"),
    BenefitItem("20% off live events", "20% discount on Guardian Live tickets", "discount", "benefit-live-discount"),
    BenefitItem("20% off masterclasses", "20% discount on Guardian Masterclasses", "discount_masterclasses", "benefit-masterclasses-discount"),
    BenefitItem("Membership card", "", "membership_card", "benefit-card"),
    BenefitItem("+1 guest", "Bring a guest to Guardian Live with the same discount and priority booking advantages", "plus_1_guest", "benefit-plus1"),
    BenefitItem("Live stream events", "Watch live streams of Flagship events", "live_stream", "benefit-stream"),
    BenefitItem("Priority booking", "Additional priority ticket booking on Guardian Live Events (before Partners)", "priority_booking", "benefit-priority-booking"),
    BenefitItem("Special thank-yous", "The occasional unique gift to thank you for your support", "complim_items", "benefit-gifts"),
    BenefitItem("Unique experiences", "Get behind the scenes of our journalism", "unique_experiences", "benefit-experiences")
  )

  case class Pricing(yearly: Int, monthly: Int) {
    lazy val yearlyMonthlyCost = (12 * monthly)
    lazy val yearlySaving = yearlyMonthlyCost - yearly
  }

  def benefitsFilter(identifiers: String*) = identifiers.flatMap { id =>
    allBenefits.find(_.identifier == id)
  }

  val friendsWithBenefits = benefitsFilter("book_tickets", "digital_digest", "video_highlights")

  val partnerWithBenefits = benefitsFilter("discount", "discount_masterclasses", "plus_1_guest", "early_booking",
    "membership_card", "live_stream", "digital_digest", "video_highlights")

  val patronWithBenefits = benefitsFilter("discount", "discount_masterclasses", "priority_booking",
    "complim_items", "unique_experiences", "plus_1_guest", "membership_card",
    "live_stream", "digital_digest", "video_highlights")

  val friendsWithBenefitsLimited = benefitsFilter("book_tickets", "digital_digest", "video_highlights")

  var partnerWithBenefitsLimited = benefitsFilter("discount", "discount_masterclasses", "early_booking",
    "plus_1_guest", "live_stream", "membership_card")

  var patronWithBenefitsLimited = benefitsFilter("priority_booking", "complim_items", "unique_experiences")

  val friendBenefits = Benefits("Benefits", friendsWithBenefits,
    None, "Become a Friend", "Stay up to date and book tickets to Guardian Live events")

  val partnerBenefits = Benefits("Friend benefits, plus…", partnerWithBenefits,
    Some(Pricing(135, 15)), "Become a Partner", "Support the Guardian and experience it brought to life, with early booking and discounted tickets")

  val patronBenefits = Benefits("Partner benefits, plus…", patronWithBenefits,
    Some(Pricing(540, 60)), "Become a Patron", "Defend the Guardian’s independence and promote the open exchange of ideas, with a backstage pass to the Guardian")

  val details = Map[Tier, Benefits](
    Tier.Friend -> friendBenefits,
    Partner -> partnerBenefits,
    Patron -> patronBenefits
  )

  val detailsLimited = Map[Tier, Seq[BenefitItem]](
    Tier.Friend -> friendsWithBenefitsLimited,
    Partner -> partnerWithBenefitsLimited,
    Patron -> patronWithBenefitsLimited
  )

}
