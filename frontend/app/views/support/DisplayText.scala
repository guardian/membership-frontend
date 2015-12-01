package views.support

import com.gu.membership.model.GBP
import com.gu.membership.salesforce.Tier
import com.gu.membership.salesforce.Tier._
import configuration.Config.zuoraFreeEventTicketsAllowance
import model.{Benefits, PaidTierDetails}
import views.support.Pricing._

object DisplayText {
  implicit class TierCopy(tier: Tier) {
    def benefits = Benefits.forTier(tier)
    def benefitsExcluding(tierOpt: Option[Tier]) = {
      val exclude = tierOpt.map(t => t.benefits).getOrElse(Seq())
      benefits.filter(!exclude.contains(_))
    }

    def cta = s"Become a ${tier.slug}"

    def detailsLimited = tier match {
      case Friend | Supporter => benefits
      case Partner => benefits.filterNot(Benefits.supporter.toSet)
      case Patron => benefits.filterNot(Benefits.partner.toSet)
      case Staff => benefits.filterNot(i => Benefits.partner.toSet.contains(i) || i == Benefits.booksOrTickets)
    }

    def leadin = tier match {
      case Supporter => "Friend benefits, plus…"
      case Partner => "Supporter benefits, plus…"
      case Patron => " Partner benefits, plus…"
      case _ => "Benefits"
    }

    def highlights = tier match {
      case Friend => List(Highlight(
        """Receive regular updates from the membership community,
          | book tickets to Guardian Live events and access the Guardian members area""".stripMargin))

      case Supporter => List(Highlight("Support our journalism and keep theguardian.com free of charge"),
        Highlight("Get access to tickets and to the live broadcast of events"))

      case Partner => List(
        Highlight(s"Includes $zuoraFreeEventTicketsAllowance tickets to Guardian Live events (or 4 Guardian-published books) per year", isNew = true),
        Highlight("Get priority booking and 20% discount on Guardian Live, Guardian Local and most Guardian Masterclasses")
      )

      case Patron => List(Highlight("Show deep support for keeping the Guardian free, open and independent."),
        Highlight("Get invited to a small number of exclusive, behind-the-scenes functions")
      )
      case _ => Nil
    }

    val chooseTierPatron = List(Highlight("Get all partner benefits, 6 tickets and 4 books plus invitations  to exclusive behind-the-scenes functions"))
  }

  implicit class PaidTierDetailsCopy(paidTierDetails: PaidTierDetails) {
    private val pricing = paidTierDetails.pricingWithFallback(GBP)

    val yearlySavingNote: Option[String] = paidTierDetails.tier match {
      case Supporter => Some("1 year membership, 2 months free")
      case Partner | Patron => Some(s"1 year membership, ${pricing.yearlySavingsInMonths} months free")
      case _ => None
    }
  }

  case class Highlight(description: String, isNew: Boolean = false)
}
