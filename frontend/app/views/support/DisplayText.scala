package views.support

import com.gu.i18n.CountryGroup
import com.gu.i18n.CountryGroup._
import com.gu.salesforce.Tier
import com.gu.salesforce.Tier._
import configuration.Config.zuoraFreeEventTicketsAllowance
import model.{Highlights, Benefits, PaidTierDetails}
import views.support.Pricing._

object DisplayText {
  implicit class TierCopy(tier: Tier) {
    def benefits = Benefits.forTier(tier)
    def benefitsExcluding(tierOpt: Option[Tier]) = {
      val exclude = tierOpt.map(t => t.benefits).getOrElse(Seq())
      benefits.filter(!exclude.contains(_))
    }

    def cta = s"Become a ${tier.slug}"

    def detailsLimited(countryGroup: CountryGroup = UK) = (tier match {
      case Friend | Supporter => benefits
      case Partner => benefits.filterNot(Benefits.supporter.toSet)
      case Patron => benefits.filterNot(Benefits.partner.toSet)
      case Staff => benefits.filterNot(i => Benefits.partner.toSet.contains(i) || i == Benefits.booksOrTickets)
    }).filterNot {
      benefit => Benefits.marketedOnlyToUK(benefit) && countryGroup != UK
    }

    def leadin = tier match {
      case Supporter => "Friend benefits, plus…"
      case Partner => "Supporter benefits, plus…"
      case Patron => " Partner benefits, plus…"
      case _ => "Benefits"
    }

    def highlights(countryGroup: CountryGroup = UK) = Highlights.forTier(tier).filterNot {
      highlight => Highlights.marketedOnlyToUK(highlight) && countryGroup != UK
    }

    val chooseTierPatron = List(Highlight("Get all partner benefits, 6 tickets and 4 books plus invitations  to exclusive behind-the-scenes functions"))
  }

  implicit class PaidTierDetailsCopy(paidTierDetails: PaidTierDetails) {
    private val pricing = paidTierDetails.gbpPricing

    val yearlySavingNote: Option[String] = paidTierDetails.tier match {
      case Supporter => Some("1 year membership, 2 months free")
      case Partner | Patron => Some(s"1 year membership, ${pricing.yearlySavingsInMonths} months free")
      case _ => None
    }
  }

  case class Highlight(description: String, isNew: Boolean = false)
}
