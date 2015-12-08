package model

import com.gu.i18n.Currency
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
  // We check the consistency of the currencies for all billing periods during the catalog's parsing
  def currencies: Set[Currency] = monthlyPlanDetails.currencies

  def byBillingPeriod(bp: BillingPeriod) = bp match {
    case Month => monthlyPlanDetails
    case Year => yearlyPlanDetails
  }
}
