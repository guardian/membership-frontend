package views.support

import model.Benefits
import com.gu.membership.salesforce.Tier._

object Prices {
  implicit class RichFloat(price: Float) {
    lazy val pretty = "£%.2f".format(price)
  }
  implicit class RichInt(price: Int) {
    lazy val pretty = "£" + price
  }

  implicit class Copy(benefits: Benefits) {
    def savingInfo: Option[String] = benefits.pricing.filter(_.hasYearlySaving).map(price => benefits.tier match {
      case Patron | Partner => "3 months free"
      case _ => s"Save ${price.yearlySaving.pretty}/year"
    })

    def yearlySavingNote: Option[String] = benefits.pricing.flatMap { _ =>
      benefits.tier match {
        case Supporter => Some("1 year membership, 2 months free")
        case Partner | Patron => Some("1 year membership, 3 months free (ends 31 Oct)")
        case _ => None
      }
    }
  }
}
