package views.support

import com.gu.membership.model.{GBP, Currency}
import model.PaidTierDetails
import views.support.Prices._

case class Pricing(currency: Currency,
                   yearly: Int,
                   monthly: Int) {

  lazy val yearlyMonthlyCost = 12 * monthly
  lazy val yearlySaving = yearlyMonthlyCost - yearly
  lazy val yearlyWith6MonthSaving = yearly / 2f
  lazy val hasYearlySaving = yearlySaving > 0
  lazy val yearlySavingsInMonths = (yearly - yearlyMonthlyCost) / monthly

  val savingInfo: Option[String] =
    if (hasYearlySaving) Some(s"Save ${yearlySaving.pretty}/year") else None
}

object Pricing {
  implicit class WithPricing(tierDetails: PaidTierDetails) {
    def pricingWithFallback(implicit currency: Currency): Pricing =
      Pricing(GBP, tierDetails.yearlyPlanDetails.priceGBP.toInt, tierDetails.monthlyPlanDetails.priceGBP.toInt)
  }
}
