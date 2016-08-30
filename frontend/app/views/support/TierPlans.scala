package views.support
import com.gu.i18n.{Country, CountryGroup, Currency}
import com.gu.memsub._
import com.gu.memsub.subsv2.Catalog.PaidMember
import com.gu.memsub.subsv2.{FreeBenefit, MembershipPlan, MonthYearPlans, PaidBenefit}
import com.gu.salesforce.Tier
import views.support.MembershipCompat._
/**
  * Hack to unify FreeMembershipPlan and PaidMembershipPlans in views
  * Can't use type classes as Twirl functions cannot accept type parameters
  */

sealed trait TierPlans {
  def tier: Tier
  def currencies: Set[Currency]
  def currency(country: Country): Option[Currency]
}

case class FreePlan(plan: MembershipPlan[FreeBenefit[Product[Tangibility]], Current]) extends TierPlans {
  override def tier = plan.tier
  override def currencies = plan.benefit.currencies
  override def currency(country: Country): Option[Currency] = CountryGroup.availableCurrency(currencies)(country)
}

case class PaidPlans(plans: MonthYearPlans[PaidMember]) extends TierPlans {
  override def tier = plans.month.tier
  override def currencies = plans.month.benefit.pricingSummary.currencies.intersect(plans.year.benefit.pricingSummary.currencies)
  override def currency(country: Country): Option[Currency] = CountryGroup.availableCurrency(currencies)(country)
}

object TierPlans {
  implicit def fromFreeMembershipPlan(plan: MembershipPlan[FreeBenefit[Product[Tangibility]], Current]): TierPlans =
    FreePlan(plan)

  implicit def fromPaidMembershipPlan(plans: MonthYearPlans[PaidMember]): TierPlans = {
    PaidPlans(plans)
  }
}
