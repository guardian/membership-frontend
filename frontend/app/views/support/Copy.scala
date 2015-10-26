package views.support

import model.Benefits
import com.gu.membership.salesforce.Tier._
import views.support.Prices._
import configuration.Config.zuoraFreeEventTicketsAllowance

object Copy {
  implicit class BenefitsCopy(benefits: Benefits) {
    val savingInfo: Option[String] = benefits.pricing.filter(_.hasYearlySaving).map(price => benefits.tier match {
      case Patron | Partner => "3 months free"
      case _ => s"Save ${price.yearlySaving.pretty}/year"
    })

    val yearlySavingNote: Option[String] = benefits.pricing.flatMap { _ =>
      benefits.tier match {
        case Supporter => Some("1 year membership, 2 months free")
        case Partner | Patron => Some("1 year membership, 3 months free (ends 31 Oct)")
        case _ => None
      }
    }

    val cta = s"Become a ${benefits.tier.slug}"

    val leadin = benefits.tier match {
      case Partner => "Supporter benefits, plus…"
      case Patron => " Partner benefits, plus…"
      case _ => "Benefits"
    }

    case class Highlight(description: String, isNew: Boolean = false)
    def highlights = benefits.tier match {
      case Friend => List(Highlight(
          """Receive regular updates from the membership community,
          | book tickets to Guardian Live events and access the Guardian members area""".stripMargin))

      case Supporter => List(Highlight("Support our journalism and keep theguardian.com free of charge"),
                             Highlight("Get access to tickets and to the live broadcast of events"))

      case Partner => List(Highlight("Get priority booking and 20% discount on Guardian Live, Guardian Local and most Guardian Masterclasses"),
                           Highlight(s"Includes $zuoraFreeEventTicketsAllowance tickets to Guardian Live events (or 4 Guardian-published books) per year", isNew = true))

      case Patron => List(Highlight("Show deep support for keeping the Guardian free, open and independent."),
                          Highlight("Get invited to a small number of exclusive, behind-the-scenes functions"))
      case _ => Nil
    }

  }

}
