package model

import com.gu.membership.salesforce.Tier

object Benefits {

  case class Benefits(leadin: String, list: Seq[(String, String)], priceMonth: Option[Int],
                      priceYear: Option[Int], saving: Option[Int], cta: String)

  val friendBenefits = Benefits("As a friend:", Seq(
    "Book tickets" -> "Book tickets to Guardian Live events",
    "Digital Digest email" -> "Receive weekly updates on the upcoming programme",
    "Exclusive offers" -> "Discounts and special offers from Guardian Discover",
    "Video highlights" -> "Watch highlights of selected Guardian Live events"
  ), None, None, None, "Become a Friend")

  val partnerBenefits = Benefits("All the benefits of a Friend plus:", Seq(
    "20% Discount" -> "20% discount on Guardian Live tickets",
    "+1 Guest" -> "Bring a guest to Guardian Live with the same discount and priority booking advantages",
    "Live stream events" -> "Watch live streams of Flagship events",
    "Membership card" -> "Unlock additional experiences",
    "Early Booking" -> "Additional priority ticket booking (one week before friends)"
  ), Some(15), Some(135), Some(45), "Become a Partner");

  val patronBenefits = Benefits("All benefits of a partner plus:", Seq(
    "Early Booking" -> "Additional priority ticket booking (one week before Partners)",
    "Unique experiences" -> "Get closer to the Guardian’s journalism and better understand the impact of our campaigns",
    "Complimentary items" -> "Thank you for your support"
  ), Some(60), Some(540), Some(180), "Become a Partner")

  val details = Map(
    Tier.Friend -> friendBenefits,
    Tier.Partner -> partnerBenefits,
    Tier.Patron -> patronBenefits
  )
}
