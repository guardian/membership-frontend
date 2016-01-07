package model

import com.gu.membership.MembershipCatalog
import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.memsub.{Month, Year, BillingPeriod}
import com.gu.salesforce.{Tier, PaidTier, FreeTier}

sealed trait PlanChoice {
  def tier: Tier
  def productRatePlanId(implicit catalog: MembershipCatalog): ProductRatePlanId
}

case class FreePlanChoice(tier: FreeTier) extends PlanChoice {
  override def productRatePlanId(implicit catalog: MembershipCatalog) =
    catalog.findFree(tier).productRatePlanId
}
case class PaidPlanChoice(tier: PaidTier, billingPeriod: BillingPeriod) extends PlanChoice {
  override def productRatePlanId(implicit catalog: MembershipCatalog) = billingPeriod match {
    case Year() => catalog.findPaid(tier).year.productRatePlanId
    case Month() => catalog.findPaid(tier).month.productRatePlanId
    case _ => throw new IllegalStateException(s"Unreachable code: Expected plan choice ${this} to be either annual or monthly, but found a ${billingPeriod.noun} billing period")
  }
}
