package model

import com.gu.memsub.BillingPeriod.{Month, Year}
import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.memsub.subsv2.Catalog
import com.gu.memsub.BillingPeriod
import com.gu.salesforce.{FreeTier, PaidTier, Tier}
import views.support.MembershipCompat._

sealed trait PlanChoice {
  def tier: Tier
  def productRatePlanId(implicit catalog: Catalog): ProductRatePlanId
}

case class FreePlanChoice(tier: FreeTier) extends PlanChoice {
  override def productRatePlanId(implicit catalog: Catalog) =
    catalog.findFree(tier).id
}
case class PaidPlanChoice(tier: PaidTier, billingPeriod: BillingPeriod) extends PlanChoice {
  override def productRatePlanId(implicit catalog: Catalog) = billingPeriod match {
    case Year => catalog.findPaid(tier).year.id
    case Month => catalog.findPaid(tier).month.id
    case _ => throw new IllegalStateException(s"Unreachable code: Expected plan choice ${this} to be either annual or monthly, but found a ${billingPeriod.noun} billing period")
  }
}


sealed trait ContributionPlanChoice {
  def productRatePlanId(implicit catalog: Catalog): ProductRatePlanId
}
case class ContributorChoice() extends ContributionPlanChoice {
  override def productRatePlanId(implicit catalog: Catalog) = catalog.contributor.id
}


