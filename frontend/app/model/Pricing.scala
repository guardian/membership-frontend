package model
import com.gu.membership.zuora.rest.{GBP, Currency}

case class Pricing(currency: Currency,
                   yearly: Int,
                   monthly: Int) {

  lazy val yearlyMonthlyCost = 12 * monthly
  lazy val yearlySaving = yearlyMonthlyCost - yearly
  lazy val yearlyWith6MonthSaving = yearly / 2f
  lazy val hasYearlySaving = yearlySaving > 0
  lazy val yearlySavingsInMonths = (yearly - yearlyMonthlyCost) / monthly
}
