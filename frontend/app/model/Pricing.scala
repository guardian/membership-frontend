package model

case class Pricing(yearly: Int, monthly: Int) {
  lazy val yearlyMonthlyCost = 12 * monthly
  lazy val yearlySaving = yearlyMonthlyCost - yearly
  lazy val yearlyWith6MonthSaving = yearly / 2f
  lazy val hasYearlySaving = yearlySaving > 0
  lazy val yearlySavingsInMonths = (yearly - yearlyMonthlyCost) / monthly
  lazy val yearlySavingsNote = {
    val s = if (yearlySavingsInMonths != 1) "s" else ""
    if (!hasYearlySaving) None else Some(s"1 year membership, $yearlySavingsInMonths month$s free")
  }
}
