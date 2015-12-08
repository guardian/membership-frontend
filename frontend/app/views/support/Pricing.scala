package views.support

import com.gu.i18n.Currency
import com.gu.membership.model.Price
import model.PaidTierDetails

case class Pricing(yearly: Price, monthly: Price) {
  require(yearly.currency == monthly.currency, "The yearly and monthly prices should have the same currency")

  lazy val currency = monthly.currency
  lazy val yearlyMonthlyPrice = monthly * 12
  lazy val yearlySaving = yearlyMonthlyPrice - yearly.amount
  lazy val yearlyWith6MonthSaving = yearly / 2f
  lazy val hasYearlySaving = yearlySaving.amount > 0
  lazy val yearlySavingsInMonths = (yearly.amount - yearlyMonthlyPrice.amount) / monthly.amount

  val savingInfo: Option[String] =
    if (hasYearlySaving) Some(s"Save ${yearlySaving.pretty}/year") else None
}

object Pricing {
  implicit class WithPricing(td: PaidTierDetails) {
    lazy val allPricing: List[Pricing] = Currency.all.flatMap(pricing)

    def unsafePriceByCurrency(currency: Currency) = pricing(currency).getOrElse {
      val ratePlanIds = Seq(td.monthlyPlanDetails.productRatePlanId, td.yearlyPlanDetails.productRatePlanId).mkString(",")
      throw new NoSuchElementException(s"Cannot find a $currency price for tier ${td.tier} (product rate plan ids: $ratePlanIds)")
    }

    def gbpPricing = Pricing(td.yearlyPlanDetails.priceGBP, td.monthlyPlanDetails.priceGBP)

    private def pricing(c: Currency): Option[Pricing] = {
      td.yearlyPlanDetails.pricingByCurrency.getPrice(c)
        .zip(td.monthlyPlanDetails.pricingByCurrency.getPrice(c))
        .map((Pricing.apply _).tupled)
        .headOption
    }
  }
}
