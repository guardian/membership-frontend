package model

import com.gu.membership.model.PaidTierPlan
import com.gu.membership.salesforce.Tier
import com.gu.membership.salesforce.Tier._
import com.gu.membership.zuora.rest
import com.gu.membership.zuora.rest.{Currency, GBP}
import services.TouchpointBackend
import Function.const

case class TierPricing(catalog: rest.ProductCatalog) {

  type ErrorReport = Map[Tier, List[String]]

  lazy val patronBenefits:Benefits = benefits(Patron)
  lazy val partnerBenefits:Benefits = benefits(Partner)
  lazy val supporterBenefits:Benefits = benefits(Supporter)
  lazy val friendBenefits: Benefits = benefits(Friend)
  lazy val staffBenefits:Benefits = benefits(Staff)

  def byTier: Either[ErrorReport, Map[Tier, Pricing]] = {
    val ePricingByTier = Tier.allPublic.filter(_.isPaid).map { t => t -> forTier(t) }.toMap

    if (ePricingByTier.exists(_._2.isLeft))
      Left(ePricingByTier.collect { case (tier, Left(errors)) => tier -> errors })
    else
      Right(ePricingByTier.collect { case (tier, Right(pricing)) => tier -> pricing })
  }

  def benefits(tier: Tier): Benefits =
    Benefits(tier, byTier.fold(const(None), { x => x.get(tier) }))

  private def forTier(tier: Tier): Either[List[String], Pricing] = {
    (findPrice(PaidTierPlan(tier, annual = true), GBP),
     findPrice(PaidTierPlan(tier, annual = false), GBP)) match {

      case (Right(annualPrice), Right(monthPrice)) =>
        Right(Pricing(annualPrice.toInt, monthPrice.toInt))
      case (annual, monthly) =>
        Left(List(annual, monthly).collect { case Left(msg) => msg })
    }
  }

  private def findPrice(plan: PaidTierPlan, currency: Currency): Either[String, Float] = {
    import utils.OptionOps
    val period = plan.billingPeriod

    for {
      ratePlan <- catalog.ratePlanByTierPlan(plan) toEither s"Cannot find a RatePlan (billingPeriod: $period)"
      ratePlanCharge <- ratePlan.findCharge(plan.billingPeriod, rest.FlatFee) toEither s"Cannot find a RatePlanCharge (billingPeriod: $period)"
      price <- ratePlanCharge.pricingSummaryParsed.getPrice(currency) toEither s"Cannot find a $currency price"
    } yield price
  }
}
