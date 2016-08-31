package views.support
import com.gu.i18n.{Country, CountryGroup, Currency, GBP}
import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.memsub._
import com.gu.memsub.subsv2.Catalog.PaidMember
import com.gu.memsub.subsv2._
import com.gu.salesforce.{FreeTier, PaidTier, Tier}

import scala.language.higherKinds
import scala.util.Try

/**
  * Created by tverran on 30/08/2016.
  */
object MembershipCompat {

  implicit class MonthYearMembership[P <: Product[Tangibility]](in: MonthYearPlans[PaidMember]) {
    def tier: PaidTier = in.month.tier

  }
  implicit class GenericMembership(in: MembershipPlan[Benefit, Status]) {
    def tier: Tier = in.benefit match {
      case PaidBenefit(com.gu.memsub.Supporter, _, _) => Tier.supporter
      case PaidBenefit(com.gu.memsub.Partner, _, _) => Tier.partner
      case PaidBenefit(com.gu.memsub.Patron, _, _) => Tier.patron
      case FreeBenefit(com.gu.memsub.Friend, _) => Tier.friend
      case FreeBenefit(com.gu.memsub.Staff, _) => Tier.staff

    }
  }

  implicit class PaidMembership(in: MembershipPlan[PaidBenefit[Product[Tangibility], BillingPeriod], Status]) {

    def currencyOrGBP(cg: Country): Currency = CountryGroup
      .byCountryCode(cg.alpha2).map(_.currency)
      .filter(in.benefit.pricingSummary.currencies.contains)
      .getOrElse(GBP)

    def tier: PaidTier = in.benefit match {
      case PaidBenefit(com.gu.memsub.Supporter, _, _) => Tier.supporter
      case PaidBenefit(com.gu.memsub.Partner, _, _) => Tier.partner
      case PaidBenefit(com.gu.memsub.Patron, _, _) => Tier.patron
    }
  }

  implicit class YMPlans(in: MonthYearPlans[PaidMember]) {

    def get(b: BillingPeriod): PaidMember[BillingPeriod] = b match {
      case Month() => in.month
      case Year() => in.year
    }
  }

  implicit class FreeMembership(in: MembershipPlan[FreeBenefit[Product[Tangibility]], Status]) {


    def currencyOrGBP(cg: Country): Currency = CountryGroup
      .byCountryCode(cg.alpha2).map(_.currency)
      .filter(in.benefit.currencies.contains)
      .getOrElse(GBP)

    def tier: FreeTier = in.benefit match {
      case FreeBenefit(com.gu.memsub.Friend, _) => Tier.friend
      case FreeBenefit(com.gu.memsub.Staff, _) => Tier.staff
    }
  }

  implicit class tierCatalog(in: Catalog) {

    def find(prpId: ProductRatePlanId) =
      Seq(in.friend, in.supporter.month, in.supporter.year, in.partner.month, in.partner.year, in.patron.month, in.patron.year)
        .find(_.productRatePlanId == prpId)

    def unsafeFind(prpId: ProductRatePlanId) = find(prpId).getOrElse(throw new Exception(s"no plan with $prpId"))

    def unsafeFindPaid(prpId: ProductRatePlanId): MembershipPlan[PaidBenefit[Product[Tangibility], BillingPeriod], Current] =
      Seq(in.supporter.month, in.supporter.year, in.partner.month, in.partner.year, in.patron.month, in.patron.year)
        .find(_.productRatePlanId == prpId).getOrElse(throw new Exception(s"no paid plan with id $prpId"))

    def unsafeFindFree(prpId: ProductRatePlanId): MembershipPlan[FreeBenefit[Product[Tangibility]], Current] =
      Seq(in.friend)
        .find(_.productRatePlanId == prpId).getOrElse(throw new Exception(s"no free plan with id $prpId"))

    def findPaid(p: PaidTier): MonthYearPlans[PaidMember] = p match {
      case Tier.Supporter() => in.supporter
      case Tier.Partner() => in.partner
      case Tier.Patron() => in.patron
    }

    def findPaid(p: ProductRatePlanId): Option[MembershipPlan[PaidBenefit[Product[Tangibility], BillingPeriod], Current]] =
      Try(unsafeFindPaid(p)).toOption

    def findFree(f: FreeTier) = f match {
      case Tier.Friend() => in.friend
      case Tier.Staff() => in.friend // fix
    }
  }

}
