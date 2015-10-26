package model

import com.gu.membership.model.PaidTierPlan
import com.gu.membership.salesforce.Tier
import com.gu.membership.salesforce.Tier._
import com.gu.membership.zuora.rest
import com.gu.membership.zuora.rest.PricingSummary

import scala.Function.const

case class TierPricing(catalog: rest.ProductCatalog) {
  import utils.OptionOps

  type ErrorReport = Map[Tier, List[String]]

  lazy val patronBenefits:Benefits = benefits(Patron)
  lazy val partnerBenefits:Benefits = benefits(Partner)
  lazy val supporterBenefits:Benefits = benefits(Supporter)
  lazy val friendBenefits: Benefits = benefits(Friend)
  lazy val staffBenefits:Benefits = benefits(Staff)

  def byTier: Either[ErrorReport, Map[Tier, InternationalPricing]] = {
    val ePricingByTier =
      Tier.allPublic.filter(_.isPaid).map { t => t -> internationalPrices(t) }.toMap

    if (ePricingByTier.exists(_._2.isLeft))
      Left(ePricingByTier.collect { case (tier, Left(errors)) => tier -> errors })
    else
      Right(ePricingByTier.collect { case (tier, Right(pricing)) => tier -> pricing })
  }

  def benefits(tier: Tier): Benefits = Benefits(tier, byTier.fold(const(None), { x => x.get(tier) }))

  private def internationalPrices(tier: Tier): Either[List[String], InternationalPricing] =
    (pricingSummary(PaidTierPlan(tier, annual = true)),
     pricingSummary(PaidTierPlan(tier, annual = false))) match {

      case (Right(annual), Right(monthly)) =>
        InternationalPricing.fromPricingSummaries(annual, monthly)
          .toEither(List("Cannot find GBP price")).e

      case (annual, monthly) =>
        Left(List(annual, monthly).collect { case Left(msg) => msg })
    }

  private def pricingSummary(plan: PaidTierPlan): Either[String, PricingSummary] = {
    val period = plan.billingPeriod

    for {
      ratePlan <- catalog.ratePlanByTierPlan(plan) toEither s"Cannot find a RatePlan (billingPeriod: $period)"
      ratePlanCharge <- ratePlan.findCharge(plan.billingPeriod, rest.FlatFee) toEither s"Cannot find a RatePlanCharge (billingPeriod: $period)"
    } yield ratePlanCharge.pricingSummaryParsed
  }
}
