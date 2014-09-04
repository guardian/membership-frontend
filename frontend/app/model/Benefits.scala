package model

import com.gu.membership.salesforce.Tier

object Benefits {

  case class Benefits(leadin: String, list: Seq[(String, String)], pricing: Option[Pricing], cta: String)

  case class Pricing(yearly: Int, monthly: Int) {
    lazy val yearlyMonthlyCost = (12 * monthly)
    lazy val yearlySaving = yearlyMonthlyCost - yearly
  }

  val friendBenefits = Benefits("As a friend:", Seq(
    "Book tickets" -> "Book tickets to Guardian Live events",
    "Digital Digest email" -> "Receive weekly updates on the upcoming programme",
    "Video highlights" -> "Watch highlights of selected Guardian Live events"
  ), None, "Become a Friend")

  val partnerBenefits = Benefits("All the benefits of a Friend plus:", Seq(
    "20% Discount" -> "20% discount on Guardian Live tickets",
    "+1 Guest" -> "Bring a guest to Guardian Live with the same discount and priority booking advantages",
    "Live stream events" -> "Watch live streams of Flagship events",
    "Membership card" -> "Unlock additional experiences",
    "Early Booking" -> "Additional priority ticket booking (one week before friends)"
  ), Some(Pricing(135, 15)), "Become a Partner");

  val patronBenefits = Benefits("All benefits of a partner plus:", Seq(
    "Early Booking" -> "Additional priority ticket booking (one week before Partners)",
    "Unique experiences" -> "Get closer to the Guardian’s journalism and better understand the impact of our campaigns",
    "Complimentary items" -> "Thank you for your support"
  ), Some(Pricing(540, 60)), "Become a Partner")

  val details = Map(
    Tier.Friend -> friendBenefits,
    Tier.Partner -> partnerBenefits,
    Tier.Patron -> patronBenefits
  )
}
