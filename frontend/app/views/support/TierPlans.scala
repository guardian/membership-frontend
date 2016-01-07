package views.support

import com.gu.membership.{FreeMembershipPlan, PaidMembershipPlans}
import com.gu.memsub.Current
import com.gu.salesforce.{FreeTier, PaidTier, Tier}

/**
  * Hack to unify FreeMembershipPlan and PaidMembershipPlans in views
  * Can't use type classes as Twirl functions cannot accept type parameters
  */

sealed trait TierPlans {
  def tier: Tier
}

case class FreePlan(plan: FreeMembershipPlan[Current, FreeTier]) extends TierPlans {
  override def tier = plan.tier
}

case class PaidPlans(plans: PaidMembershipPlans[Current, PaidTier]) extends TierPlans {
  override def tier = plans.tier
}

object TierPlans {
  implicit def fromFreeMembershipPlan(plan: FreeMembershipPlan[Current, FreeTier]): TierPlans =
    FreePlan(plan)

  implicit def fromPaidMembershipPlan(plans: PaidMembershipPlans[Current, PaidTier]): TierPlans = {
    PaidPlans(plans)
  }
}
