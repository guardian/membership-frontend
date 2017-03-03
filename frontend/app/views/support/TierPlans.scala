package views.support
import com.gu.i18n.{Country, CountryGroup, Currency}
import com.gu.memsub.Benefit.PaidMemberTier
import com.gu.memsub._
import com.gu.memsub.subsv2.CatalogPlan.{PaidMember, Partner}
import com.gu.memsub.subsv2._
import com.gu.salesforce.Tier
import views.support.MembershipCompat._

import scala.language.implicitConversions
/**
  * Hack to unify FreeMembershipPlan and PaidMembershipPlans in views
  * Can't use type classes as Twirl functions cannot accept type parameters
  */

sealed trait TierPlans {
  def tier: Tier
  def currencies: Set[Currency]
  def currency(country: Country): Option[Currency]
}

case class FreePlan(plan: CatalogPlan.FreeMember) extends TierPlans {
  override def tier = plan.tier
  override def currencies = plan.charges.currencies
  override def currency(country: Country): Option[Currency] = CountryGroup.availableCurrency(currencies)(country)
}

case class PaidPlans[M <: PaidMembershipPlans[PaidMemberTier]](plans: M) extends TierPlans {
  override def tier = plans.month.tier
  override def currencies = plans.month.charges.price.currencies.intersect(plans.year.charges.price.currencies)
  override def currency(country: Country): Option[Currency] = CountryGroup.availableCurrency(currencies)(country)
}

object TierPlans {
  implicit def fromFreeMembershipPlan(plan: CatalogPlan.FreeMember): TierPlans =
    FreePlan(plan)

  implicit def fromPaidMembershipPlan(plans: PaidMembershipPlans[Benefit.PaidMemberTier]): TierPlans = {
    PaidPlans(plans)
  }
}
