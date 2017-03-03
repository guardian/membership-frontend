package views.support

import com.gu.i18n.Currency.GBP
import com.gu.i18n.{Country, CountryGroup, Currency}
import com.gu.memsub.BillingPeriod.{Month, Year}
import com.gu.memsub.Product.Membership
import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.memsub.subsv2.{CatalogPlan, _}
import com.gu.memsub.{Subscription => _, _}
import com.gu.salesforce.{FreeTier, PaidTier, Tier}
import Benefit._

import scala.language.higherKinds
import scala.util.Try

/**
  * this is a temporary situation until membership is refactored more properly to understand the new membership-common models
  * please delete me and refactor all the things properly!
  */
object MembershipCompat {

  implicit class MonthYearMembership(in: PaidMembershipPlans[Benefit.PaidMemberTier]) {
    def tier: PaidTier = in.month.tier
  }

  implicit class GenericMembership(in: Plan[Membership, ChargeList with SingleBenefit[MemberTier]]) {

    def currency: Currency = in.charges.currencies.head

    def tier: Tier = in.charges.benefit match {
      case Supporter => Tier.supporter
      case Partner => Tier.partner
      case Patron => Tier.patron
      case Friend => Tier.friend
      case Staff => Tier.staff

    }
  }

  implicit class PaidMembership(in: Plan[Membership, PaidCharge[PaidMemberTier, BillingPeriod]]) {

    def currencyOrGBP(cg: Country): Currency = CountryGroup
      .byCountryCode(cg.alpha2).map(_.currency)
      .filter(in.charges.price.currencies.contains)
      .getOrElse(GBP)

    def tier: PaidTier = in.charges.benefit match {
      case Supporter => Tier.supporter
      case Partner => Tier.partner
      case Patron => Tier.patron
    }
  }

  implicit class YMPlans(in: PaidMembershipPlans[Benefit.PaidMemberTier]) {

    def get(b: BillingPeriod): CatalogPlan.PaidMember[BillingPeriod] = b match {
      case Month => in.month
      case Year => in.year
      case _ => throw new RuntimeException("can't have quarterly membership")
    }
  }

  implicit class FreeMembership(in: CatalogPlan.FreeMember) {

    def currencyOrGBP(cg: Country): Currency = CountryGroup
      .byCountryCode(cg.alpha2).map(_.currency)
      .filter(in.charges.currencies.contains)
      .getOrElse(GBP)

    def tier: FreeTier = in.charges.benefit match {
      case Friend => Tier.friend
      case Staff => Tier.staff
    }
  }

  implicit class tierCatalog(in: Catalog) {

    def find(prpId: ProductRatePlanId) =
      Seq(in.friend, in.supporter.month, in.supporter.year, in.partner.month, in.partner.year, in.patron.month, in.patron.year)
        .find(_.id == prpId)

    def unsafeFind(prpId: ProductRatePlanId) =
      find(prpId).getOrElse(throw new Exception(s"no plan with $prpId"))

    def unsafeFindPaid(prpId: ProductRatePlanId): CatalogPlan.PaidMember[BillingPeriod] =
      Seq(in.supporter.month, in.supporter.year, in.partner.month, in.partner.year, in.patron.month, in.patron.year)
        .find(_.id == prpId).getOrElse(throw new Exception(s"no paid plan with id $prpId"))

    def unsafeFindFree(prpId: ProductRatePlanId): CatalogPlan.FreeMember = Seq(in.friend, in.staff)
        .find(_.id == prpId).getOrElse(throw new Exception(s"no free plan with id $prpId"))

    def findPaid(p: PaidTier): PaidMembershipPlans[Benefit.PaidMemberTier] = p match {
      case Tier.Supporter() => in.supporter
      case Tier.Partner() => in.partner
      case Tier.Patron() => in.patron
    }

    def findPaid(p: ProductRatePlanId): Option[CatalogPlan.PaidMember[BillingPeriod]] =
      Try(unsafeFindPaid(p)).toOption

    def findFree(f: FreeTier) = f match {
      case Tier.Friend() => in.friend
      case Tier.Staff() => in.staff
    }
  }

}
