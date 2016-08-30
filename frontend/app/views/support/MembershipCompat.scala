package views.support
import com.gu.memsub._
import com.gu.memsub.subsv2.Catalog.PaidMember
import com.gu.memsub.subsv2._
import com.gu.salesforce.{FreeTier, PaidTier, Tier}

/**
  * Created by tverran on 30/08/2016.
  */
object MembershipCompat {

  implicit class MonthYearMembership[P <: Product[Tangibility]](in: MonthYearPlans[PaidMember]) {
    def tier: PaidTier = in.month.tier

  }

  implicit class PaidMembership(in: MembershipPlan[PaidBenefit[Product[Tangibility], BillingPeriod], Current]) {
    def tier: PaidTier = in.benefit match {
      case PaidBenefit(com.gu.memsub.Supporter, _, _) => Tier.supporter
      case PaidBenefit(com.gu.memsub.Partner, _, _) => Tier.partner
      case PaidBenefit(com.gu.memsub.Patron, _, _) => Tier.patron
    }
  }

  implicit class FreeMembership(in: MembershipPlan[FreeBenefit[Product[Tangibility]], Current]) {
    def tier: FreeTier = in.benefit match {
      case FreeBenefit(com.gu.memsub.Friend, _) => Tier.friend
      case FreeBenefit(com.gu.memsub.Staff, _) => Tier.staff
    }
  }

  implicit class tierCatalog(in: Catalog) {
    def findPaid(p: PaidTier): MonthYearPlans[PaidMember] = p match {
      case Tier.Supporter() => in.supporter
      case Tier.Partner() => in.partner
      case Tier.Patron() => in.patron
    }
    def findFree(f: FreeTier) = f match {
      case Tier.Friend() => in.friend
      case Tier.Staff() => in.friend // fix
    }
  }

}
