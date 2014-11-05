package model

import com.gu.membership.salesforce.Tier

object Benefits {

  case class Benefits(
    leadin: String,
    list: Seq[(String, String, String)],
    pricing: Option[Pricing],
    cta: String,
    desc: String,
    leadBenefits: Integer // number of items to highlight
  )

  // identifier is used for CSS classes as well as within this file
  case class BenefitItem(title: String, description: String, identifier: String)  {
    def list = (title, description, identifier)
  }

  val allBenefits = Seq(
    BenefitItem("Book tickets", "Book tickets to Guardian Live events", "book_tickets"),
    BenefitItem("Membership email updates", "Receive regular updates on the upcoming programme", "digital_digest"),
    BenefitItem("Video highlights", "Watch highlights of selected Guardian Live events", "video_highlights"),
    BenefitItem("Early Booking", "Early ticket booking (before Friends)", "early_booking"),
    BenefitItem("20% discount", "20% discount on Guardian Live tickets", "discount"),
    BenefitItem("Membership card", "", "membership_card"),
    BenefitItem("+1 guest", "Bring a guest to Guardian Live with the same discount and priority booking advantages", "plus_1_guest"),
    BenefitItem("Live stream events", "Watch live streams of Flagship events", "live_stream"),
    BenefitItem("Priority Booking", "Additional priority ticket booking (before Partners)", "priority_booking"),
    BenefitItem("20% discount", "20% discount on Guardian Live tickets", "discount_patron"),
    BenefitItem("Special thank-yous", "The occasional unique gift to thank you for your support", "complim_items"),
    BenefitItem("Unique experiences", "Get behind the scenes of our journalism", "unique_experiences")
  )

  case class Pricing(yearly: Int, monthly: Int) {
    lazy val yearlyMonthlyCost = (12 * monthly)
    lazy val yearlySaving = yearlyMonthlyCost - yearly
  }

  val friendsWithBenefits = Seq(
    allBenefits.find(_.identifier.equals("book_tickets")),
    allBenefits.find(_.identifier.equals("digital_digest")),
    allBenefits.find(_.identifier.equals("video_highlights"))
  ).flatten

  val partnerWithBenefits = Seq(
    allBenefits.find(_.identifier.equals("discount")),
    allBenefits.find(_.identifier.equals("plus_1_guest")),
    allBenefits.find(_.identifier.equals("early_booking")),
    allBenefits.find(_.identifier.equals("membership_card")),
    allBenefits.find(_.identifier.equals("live_stream")),
    allBenefits.find(_.identifier.equals("digital_digest")),
    allBenefits.find(_.identifier.equals("video_highlights"))
  ).flatten

  val patronWithBenefits = Seq(
    allBenefits.find(_.identifier.equals("discount_patron")),
    allBenefits.find(_.identifier.equals("priority_booking")),
    allBenefits.find(_.identifier.equals("complim_items")),
    allBenefits.find(_.identifier.equals("unique_experiences")),
    allBenefits.find(_.identifier.equals("plus_1_guest")),
    allBenefits.find(_.identifier.equals("membership_card")),
    allBenefits.find(_.identifier.equals("live_stream")),
    allBenefits.find(_.identifier.equals("digital_digest")),
    allBenefits.find(_.identifier.equals("video_highlights"))
  ).flatten

  val friendBenefits = Benefits("As a friend:", friendsWithBenefits.map(_.list),
    None, "Become a Friend", "Stay up to date and book tickets to Guardian Live events", 3)

  val partnerBenefits = Benefits("All the benefits of a Friend plus:", partnerWithBenefits.map(_.list),
    Some(Pricing(135, 15)), "Become a Partner", "Get closer to the stories and experience the Guardian brought to life, with early booking and discounted tickets", 5);

  val patronBenefits = Benefits("All benefits of a partner plus:", patronWithBenefits.map(_.list),
    Some(Pricing(540, 60)), "Become a Patron", "Support the Guardian’s mission of promoting the open exchange of ideas, with a backstage pass to the Guardian", 4)

  val details = Map(
    Tier.Friend -> friendBenefits,
    Tier.Partner -> partnerBenefits,
    Tier.Patron -> patronBenefits
  )
}
