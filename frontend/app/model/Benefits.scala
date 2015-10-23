package model

import com.gu.membership.salesforce.Tier
import com.gu.membership.salesforce.Tier._
import configuration.Config.zuoraFreeEventTicketsAllowance

case class Benefits(tier: Tier, pricing: Option[Pricing]) {
  import Benefits._

  val list = tier match {
    case Friend => friendBenefitsList
    case Supporter => supporterBenefitsList
    case Partner => partnerBenefitsList
    case Patron => patronBenefitsList
    case Staff => staffBenefitsList
  }

  val detailsLimited = tier match {
    case Friend => friendBenefitsList
    case Supporter => supporterBenefitsList
    case Partner => uniqueBenefits(partnerBenefitsList, supporterBenefitsList)
    case Patron => uniqueBenefits(patronBenefitsList, partnerBenefitsList)
    case Staff => uniqueBenefits(partnerBenefitsList, supporterBenefitsList)
                    .filterNot(_.identifier == "books_or_tickets")
  }
}

object Benefits {
  val DiscountTicketTiers = Set[Tier](Staff, Partner, Patron)
  val ComplimenataryTicketTiers = Set[Tier](Partner, Patron)

  case class Item(identifier: String, title: String, isNew: Boolean = false)

  val allBenefits = Seq(
    Item("welcome_pack", "Welcome pack, card and gift"),
    Item("access_tickets", "Access to tickets"),
    Item("live_stream", "Access to live stream"),
    Item("email_updates", "Regular events email"),
    Item("offers_competitions", "Offers and competitions"),
    Item("priority_booking", "48hrs priority booking"),
    Item("no_booking_fees", "No booking fees"),
    Item("guest", "Bring a guest"),
    Item("books_or_tickets", s"$zuoraFreeEventTicketsAllowance tickets or 4 books", isNew = true),
    Item("books_and_tickets", s"$zuoraFreeEventTicketsAllowance tickets and 4 books", isNew = true),
    Item("discount", "20% discount for you and a guest"),
    Item("unique_experiences", "Exclusive behind-the-scenes functions")
  )

  private def benefitsFilter(identifiers: String*) = identifiers.flatMap { id =>
    allBenefits.find(_.identifier == id)
  }

  private def uniqueBenefits(benefits: Seq[Item], exclude: Seq[Item]) = {
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

  val comparisonBasicList = benefitsFilter(
    "welcome_pack",
    "live_stream",
    "access_tickets",
    "offers_competitions",
    "email_updates"
  )

  val staffBenefitsList = uniqueBenefits(partnerBenefitsList,supporterBenefitsList).filterNot(_.identifier == "books_or_tickets")

  val comparisonHiglightsList = benefitsFilter(
    "books_or_tickets",
    "priority_booking",
    "discount",
    "unique_experiences"
  )
}
