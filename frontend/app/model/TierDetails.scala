package model

import com.gu.membership.model.{BillingPeriod, Month, Year}
import com.gu.membership.salesforce.{FreeTier, PaidTier, Tier}

sealed trait TierDetails {
  def tier: Tier
}

case class FreeTierDetails(planDetails: FreeTierPlanDetails) extends TierDetails {
  override def tier: FreeTier = planDetails.plan.tier
}

case class PaidTierDetails(monthlyPlanDetails: PaidTierPlanDetails,
                           yearlyPlanDetails: PaidTierPlanDetails) extends TierDetails {

  require(monthlyPlanDetails.plan.tier == yearlyPlanDetails.plan.tier)
  override def tier: PaidTier = monthlyPlanDetails.plan.tier

  def byBillingPeriod(bp: BillingPeriod) = bp match {
    case Month => monthlyPlanDetails
    case Year => yearlyPlanDetails
  }
}
