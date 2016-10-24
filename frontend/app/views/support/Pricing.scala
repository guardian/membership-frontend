package views.support

import com.gu.i18n.{Currency, GBP}
import com.gu.memsub.subsv2.CatalogPlan.PaidMember
import com.gu.memsub.subsv2.MonthYearPlans
import com.gu.memsub.{Current, Price}
import com.gu.salesforce.PaidTier

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
  implicit class WithPricing(plans: MonthYearPlans[PaidMember]) {
    lazy val allPricing: List[Pricing] = Currency.all.flatMap(pricing)

    def unsafePriceByCurrency(currency: Currency) = pricing(currency).getOrElse {
      throw new NoSuchElementException(
        s"Cannot find a $currency price for tier ${plans.month.charges.benefit}")
    }

    def gbpPricing = Pricing(
      plans.year.charges.gbpPrice,
      plans.month.charges.gbpPrice
    )

    def pricingByCurrencyOrGBP(currency: Currency) = pricing(currency).getOrElse(gbpPricing)

    private def pricing(c: Currency): Option[Pricing] = {
      plans.year.charges.price.getPrice(c)
        .zip(plans.month.charges.price.getPrice(c))
        .map((Pricing.apply _).tupled)
        .headOption
    }
  }
}
