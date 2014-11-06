package model

import com.gu.membership.salesforce.Tier

object Benefits {

// List parameters includes three property and are heading, copy and icons
  case class Benefits(
    leadin: String,
    list: Seq[(String, String, String)],
    pricing: Option[Pricing],
    cta: String,
    desc: String
  )

  case class Pricing(yearly: Int, monthly: Int) {
    lazy val yearlyMonthlyCost = (12 * monthly)
    lazy val yearlySaving = yearlyMonthlyCost - yearly
  }

  val friendBenefits = Benefits("As a Friend:", Seq(
    ("Book tickets", "Book tickets to Guardian Live events", "book_tickets"),
    ("Membership email updates", "Receive regular updates on the upcoming programme", "digital_digest"),
    ("Video highlights", "Watch highlights of selected Guardian Live events", "video_highlights")
  ), None, "Become a Friend", "Stay up to date and book tickets to Guardian Live events")

  val partnerBenefits = Benefits("All the benefits of a Friend plus:", Seq(
    ("20% discount", "20% discount on Guardian Live tickets", "discount"),
    ("+1 guest", "Bring a guest to Guardian Live with the same discount and priority booking advantages", "plus_1_guest"),
    ("Live stream events", "Watch live streams of Flagship events", "live_stream"),
    ("Membership card", "", "membership_card"),
    ("Early booking", "Early ticket booking (before Friends)", "early_booking")
  ), Some(Pricing(135, 15)), "Become a Partner", "Get closer to the stories and experience the Guardian brought to life, with early booking and discounted tickets");

  val patronBenefits = Benefits("All benefits of a Partner plus:", Seq(
    ("Priority booking", "Additional priority ticket booking (before Partners)", "priority_booking"),
    ("Unique experiences", "Get behind the scenes of our journalism", "unique_experiences"),
    ("Special thank-yous", "The occasional unique gift to thank you for your support", "complim_items")
  ), Some(Pricing(540, 60)), "Become a Patron", "Support the Guardian’s mission of promoting the open exchange of ideas, with a backstage pass to the Guardian")

  val details = Map(
    Tier.Friend -> friendBenefits,
    Tier.Partner -> partnerBenefits,
    Tier.Patron -> patronBenefits
  )
}
