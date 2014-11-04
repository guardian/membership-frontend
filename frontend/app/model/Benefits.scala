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

  case class BenefitItem(title: String, description: String, iconClass: String)  {
    def list = (title, description, iconClass)
  }

  // changing item order here will break things
  // use the Benefits objects below to re-order
  val allBenefits: Seq[BenefitItem] = Seq(
    // friend
    BenefitItem("Book tickets", "Book tickets to Guardian Live events", "book_tickets"), // 0
    BenefitItem("Membership email updates", "Receive regular updates on the upcoming programme", "digital_digest"), // 1
    BenefitItem("Video highlights", "Watch highlights of selected Guardian Live events", "video_highlights"), // 2
    // partner
    BenefitItem("Early Booking", "Early ticket booking (before Friends)", "early_booking"), // 3
    BenefitItem("20% discount", "20% discount on Guardian Live tickets", "discount"), // 4
    BenefitItem("Membership card", "", "membership_card"), // 5
    BenefitItem("+1 guest", "Bring a guest to Guardian Live with the same discount and priority booking advantages", "plus_1_guest"), // 6
    BenefitItem("Live stream events", "Watch live streams of Flagship events", "live_stream"), // 7
    // patron
    BenefitItem("Priority Booking", "Additional priority ticket booking (before Partners)", "priority_booking"), // 8
    BenefitItem("20% discount", "20% discount on Guardian Live tickets", "discount_patron"), // 9
    BenefitItem("Special thank-yous", "The occasional unique gift to thank you for your support", "complim_items"), // 10
    BenefitItem("Unique experiences", "Get behind the scenes of our journalism", "unique_experiences") // 11
  )

  case class Pricing(yearly: Int, monthly: Int) {
    lazy val yearlyMonthlyCost = (12 * monthly)
    lazy val yearlySaving = yearlyMonthlyCost - yearly
  }

  val friendBenefits = Benefits("As a friend:", Seq(
   allBenefits(0).list,
   allBenefits(1).list,
   allBenefits(2).list
  ), None, "Become a Friend", "Stay up to date and book tickets to Guardian Live events", 3)

  // includes some (not all) friend benefits
  val partnerBenefits = Benefits("All the benefits of a Friend plus:", Seq(
    allBenefits(4).list,
    allBenefits(6).list,
    allBenefits(3).list,
    allBenefits(5).list,
    allBenefits(7).list,
    allBenefits(1).list,
    allBenefits(2).list
  ), Some(Pricing(135, 15)), "Become a Partner", "Get closer to the stories and experience the Guardian brought to life, with early booking and discounted tickets", 5);

  // includes some (not all) friend and partner benefits
  val patronBenefits = Benefits("All benefits of a partner plus:", Seq(
    allBenefits(9).list,
    allBenefits(8).list,
    allBenefits(10).list,
    allBenefits(11).list,
    allBenefits(6).list,
    allBenefits(5).list,
    allBenefits(7).list,
    allBenefits(1).list,
    allBenefits(2).list
  ), Some(Pricing(540, 60)), "Become a Patron", "Support the Guardian’s mission of promoting the open exchange of ideas, with a backstage pass to the Guardian", 4)

  val details = Map(
    Tier.Friend -> friendBenefits,
    Tier.Partner -> partnerBenefits,
    Tier.Patron -> patronBenefits
  )
}
