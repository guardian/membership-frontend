package views.support

import com.gu.i18n.Currency
import com.gu.membership.PaidMembershipPlans
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
    if (hasYearlySaving) Some(s"Save ${yearlySaving.pretty}/year vs monthly installments") else None
}

object Pricing {
  implicit class WithPricing(plans: PaidMembershipPlans[Current, PaidTier]) {
    lazy val allPricing: List[Pricing] = Currency.all.flatMap(pricing)

    def unsafePriceByCurrency(currency: Currency) = pricing(currency).getOrElse {
      val ratePlanIds = Seq(plans.month.productRatePlanId, plans.year.productRatePlanId).mkString(",")
      throw new NoSuchElementException(
        s"Cannot find a $currency price for tier ${plans.tier} (product rate plan ids: $ratePlanIds)")
    }

    def gbpPricing = Pricing(plans.year.priceGBP, plans.month.priceGBP)

    def pricingByCurrencyOrGBP(currency: Currency) = pricing(currency).getOrElse(gbpPricing)

    private def pricing(c: Currency): Option[Pricing] = {
      plans.year.pricing.getPrice(c)
        .zip(plans.month.pricing.getPrice(c))
        .map((Pricing.apply _).tupled)
        .headOption
    }
  }
}
