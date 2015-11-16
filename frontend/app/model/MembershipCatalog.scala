package model

import com.gu.membership.model._
import com.gu.membership.salesforce.Tier._
import com.gu.membership.salesforce.{FreeTier, PaidTier, Tier}
import com.gu.membership.touchpoint.TouchpointBackendConfig.BackendType
import com.gu.membership.zuora.rest
import com.gu.membership.zuora.rest.{PricingSummary, ProductRatePlan, ProductRatePlanCharge}
import com.gu.membership.zuora.soap.models.SubscriptionDetails
import configuration.RatePlanIds
import model.MembershipCatalog.{MembershipCatalogException, ProductRatePlanId}

import scalaz._
import scalaz.syntax.applicative._
import scalaz.syntax.validation._
import scalaz.syntax.std.list._
import scalaz.Scalaz.listInstance
import scalaz.Validation.FlatMap._

object MembershipCatalog {
  type Err = (TierPlan, String)
  type Val[T] = ValidationNel[Err, T]
  type ProductRatePlanId = String

  case class MembershipCatalogException(msg: String)(implicit bt: BackendType) extends RuntimeException {
    override def getMessage: String = s"[${bt.name} backend] $msg"
  }

  implicit class ValWithAssert[T](validation: Val[T]) {
    def assert(err: T => NonEmptyList[Err])(p: T => Boolean) =
      validation.flatMap { t =>
        validation.ensure(err(t))(p)
      }
  }

  def apply(
    _friend: FreeTierDetails,
    _staff: FreeTierDetails,
    _supporter: PaidTierDetails,
    _partner: PaidTierDetails,
    _patron: PaidTierDetails,
    tierPlanDetailsMap: Map[ProductRatePlanId, TierPlanDetails],
    _backendType: BackendType
  ): MembershipCatalog = new MembershipCatalog {

    override def staff = _staff
    override def patron = _patron
    override def partner = _partner
    override def friend = _friend
    override def supporter = _supporter
    override def backendType = _backendType
    override def tierPlanDetails(ratePlanId: ProductRatePlanId): Option[TierPlanDetails] = tierPlanDetailsMap.get(ratePlanId)
  }

  def fromZuora(ratePlanIds: RatePlanIds)(ratePlans: Seq[rest.ProductRatePlan])(implicit bt: BackendType): Val[MembershipCatalog] = {
    val ratePlansById = ratePlans.map(p => p.id -> p).toMap
    def byId(tierPlan: TierPlan, id: ProductRatePlanId): Val[ProductRatePlan] =
      ratePlansById
        .get(id)
        .map(_.successNel)
        .getOrElse((tierPlan -> s"""Failed to find the plan with id "$id"""").failureNel)

    def planCharge(plan: ProductRatePlan) = plan.productRatePlanCharges match {
      case Seq(c) => c.successNel
      case Nil => s"Could not find any rate plan charge for ${plan.id}".failureNel
      case cs => s"Ambiguous rate plan charges found for ${plan.id}: ${cs.mkString(", ")}".failureNel
    }

    def planAndCharge(tierPlan: TierPlan, ratePlanId: String): Val[(ProductRatePlan, ProductRatePlanCharge)] =
      byId(tierPlan, ratePlanId).flatMap { plan =>
        planCharge(plan).leftMap(_.map(tierPlan -> _)).map {charge =>
          (plan, charge)
        }
      }

    def freeTierPlanDetails(tierPlan: FreeTierPlan, ratePlanId: ProductRatePlanId): Val[FreeTierPlanDetails] =
      byId(tierPlan, ratePlanId).map { plan =>
        FreeTierPlanDetails(tierPlan, plan.id)
      }

    def paidTierPlanDetails(tierPlan: PaidTierPlan, ratePlanId: ProductRatePlanId): Val[PaidTierPlanDetails] =
      planAndCharge(tierPlan, ratePlanId).flatMap { case (plan, charge) =>
        val summary: Either[NonEmptyList[Err], PricingSummary] =
          PricingSummary.fromRatePlanCharge(charge)
            .left.map(_.map(tierPlan -> _).toList.toNel.get)

        Validation.fromEither(summary)
          .map { summary =>
            PaidTierPlanDetails(tierPlan, plan.id, summary)
          }
      }

    def currenciesMismatchErr(tierDetails: PaidTierDetails): NonEmptyList[Err] = {
      val msg = "The supported currencies for the different plans do not match"
      NonEmptyList(
        tierDetails.monthlyPlanDetails.plan -> msg,
        tierDetails.yearlyPlanDetails.plan -> msg
      )
    }

    def currenciesMatch(tierDetails: PaidTierDetails): Boolean =
      tierDetails.monthlyPlanDetails.currencies == tierDetails.yearlyPlanDetails.currencies

    object PublicPlans {
      val ids = ratePlanIds
      val friend = freeTierPlanDetails(FriendTierPlan.current, ids.friend)
      val staff = freeTierPlanDetails(StaffPlan, ids.staff)
      val supporterM = paidTierPlanDetails(PaidTierPlan.monthly(Supporter, Current), ids.supporterMonthly)
      val supporterY = paidTierPlanDetails(PaidTierPlan.yearly(Supporter, Current), ids.supporterYearly)
      val partnerM = paidTierPlanDetails(PaidTierPlan.monthly(Partner, Current), ids.partnerMonthly)
      val partnerY = paidTierPlanDetails(PaidTierPlan.yearly(Partner, Current), ids.partnerYearly)
      val patronM = paidTierPlanDetails(PaidTierPlan.monthly(Patron, Current), ids.patronMonthly)
      val patronY = paidTierPlanDetails(PaidTierPlan.yearly(Patron, Current), ids.patronYearly)
      val all = List(friend, staff, supporterM, supporterY, partnerM, partnerY, patronM, patronY)
    }

    object LegacyPlans {
      val ids = ratePlanIds.legacy
      val friend = freeTierPlanDetails(FriendTierPlan.legacy, ids.friend)
      val supporterM = paidTierPlanDetails(PaidTierPlan.monthly(Supporter, Legacy), ids.supporterMonthly)
      val supporterY = paidTierPlanDetails(PaidTierPlan.yearly(Supporter, Legacy), ids.supporterYearly)
      val partnerM = paidTierPlanDetails(PaidTierPlan.monthly(Partner, Legacy), ids.partnerMonthly)
      val partnerY = paidTierPlanDetails(PaidTierPlan.yearly(Partner, Legacy), ids.partnerYearly)
      val patronM = paidTierPlanDetails(PaidTierPlan.monthly(Patron, Legacy), ids.patronMonthly)
      val patronY = paidTierPlanDetails(PaidTierPlan.yearly(Patron, Legacy), ids.patronYearly)
      val all = List(friend, supporterM, supporterY, partnerM, partnerY, patronM, patronY)
    }

    val tierPlanDetailsMap: Val[Map[ProductRatePlanId, TierPlanDetails]] =
      //The failed public plans are discarded as they are going to be accumulated while the tier details are computed
      Applicative[Val].sequence(PublicPlans.all.filter(_.isSuccess) ++ LegacyPlans.all)
        .map(_.map { d => d.productRatePlanId -> d }.toMap)

    def paidTierDetails(monthly: Val[PaidTierPlanDetails], yearly: Val[PaidTierPlanDetails]): Val[PaidTierDetails] =
      (monthly |@| yearly)(PaidTierDetails).assert(currenciesMismatchErr)(currenciesMatch)

    object Details {
      private val P = PublicPlans
      val friend = P.friend.map(FreeTierDetails)
      val staff = P.staff.map(FreeTierDetails)
      val supporter = paidTierDetails(P.supporterM, P.supporterY)
      val partner = paidTierDetails(P.partnerM, P.partnerY)
      val patron = paidTierDetails(P.patronM, P.patronY)
    }

    ( Details.friend |@| Details.staff |@| Details.supporter |@| Details.partner |@| Details.patron |@|
      tierPlanDetailsMap |@| bt.successNel
    )(MembershipCatalog.apply)
  }

  def unsafeFromZuora(ratePlanIds: RatePlanIds)(ratePlans: Seq[rest.ProductRatePlan])(implicit bt: BackendType): MembershipCatalog = {
    fromZuora(ratePlanIds)(ratePlans) match {
      case Success(catalog) => catalog
      case Failure(errs) =>
        val msg = s"The catalog is inconsistent:\n" + errs.list.map(errorLine).mkString("\n")
        throw MembershipCatalogException(msg)
    }
  }

  def errorLine(p: (TierPlan, String)) = s"  - ${p._1}: ${p._2}"
}

trait MembershipCatalog {
  def friend: FreeTierDetails
  def staff: FreeTierDetails
  def supporter: PaidTierDetails
  def partner: PaidTierDetails
  def patron: PaidTierDetails
  def tierPlanDetails(productRatePlanId: ProductRatePlanId): Option[TierPlanDetails]
  implicit def backendType: BackendType

  def unsafeTierPlan(productRatePlanId: ProductRatePlanId): TierPlan = tierPlanDetails(productRatePlanId).map(_.plan).getOrElse {
    throw MembershipCatalogException(s"Cannot find TierPlanDetails for ratePlanId: $productRatePlanId")
  }

  def unsafePaidTierPlan(productRatePlanId: ProductRatePlanId): PaidTierPlan = tierPlanDetails(productRatePlanId).map(_.plan) match {
    case Some(p: PaidTierPlan) => p
    case _ => throw MembershipCatalogException(s"Cannot find PaidTierPlanDetails for ratePlanId: $productRatePlanId")
  }

  def unsafePaidTierPlanDetails(subs: SubscriptionDetails): PaidTierPlanDetails = {
     val prpId = subs.productRatePlanId
     val plan = unsafePaidTierPlan(prpId)
     PaidTierPlanDetails(plan, prpId, PricingSummary(Map(subs.currency -> subs.planAmount)))
  }

  def ratePlanId(plan: TierPlan): String = planDetails(plan).productRatePlanId

  def publicTierDetails(tier: Tier): TierDetails = tier match {
    case Friend => friend
    case Staff => staff
    case Supporter => supporter
    case Partner => partner
    case Patron => patron
  }

  def freeTierDetails(freeTier: FreeTier): FreeTierDetails = freeTier match {
    case Friend => friend
    case Staff => staff
  }

  def paidTierDetails(paidTier: PaidTier): PaidTierDetails = paidTier match {
    case Supporter => supporter
    case Partner => partner
    case Patron => patron
  }

  def planDetails(tierPlan: TierPlan): TierPlanDetails = tierPlan match {
    case FriendTierPlan(Current) => friend.planDetails
    case StaffPlan => staff.planDetails
    case PaidTierPlan(Supporter, Year, Current) => supporter.yearlyPlanDetails
    case PaidTierPlan(Supporter, Month, Current) => supporter.monthlyPlanDetails
    case PaidTierPlan(Partner, Year, Current) => partner.yearlyPlanDetails
    case PaidTierPlan(Partner, Month, Current) => partner.monthlyPlanDetails
    case PaidTierPlan(Patron, Year, Current) => patron.yearlyPlanDetails
    case PaidTierPlan(Patron, Month, Current) => patron.monthlyPlanDetails
    case _ => throw MembershipCatalogException("planDetails called with a legacy TierPlan")
  }
}
