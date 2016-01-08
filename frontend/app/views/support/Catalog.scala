package views.support

import com.gu.i18n._
import com.gu.membership.MembershipCatalog.{PlanId, Val}
import com.gu.membership.{FreeMembershipPlan, MembershipCatalog, MembershipPlan, PaidMembershipPlan}
import com.gu.memsub.Status
import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.salesforce.{PaidTier, Tier}
import model._
import play.api.libs.json._

import scalaz.NonEmptyList
import scalaz.syntax.std.option._

object Catalog {
  case class Diagnostic(comparisonTable: Option[ComparisonTable], errorTables: Seq[ErrorTable])

  object Diagnostic {
    import MembershipCatalog.Val
    def fromCatalogs(testCat: Val[MembershipCatalog], normalCat: Val[MembershipCatalog]): Diagnostic = {
      val test = "test"
      val normal = "normal"

      val comparisonTable =
        (ComparisonTable.fromCatalog(testCat, test), ComparisonTable.fromCatalog(normalCat, normal)) match {
          case (Some(t), Some(n)) => Some(t.interleave(n))
          case (Some(t), _) => Some(t)
          case (_, Some(n)) => Some(n)
          case _ => None
        }

      val errorsTables =
        Seq(ErrorTable.fromCatalog(testCat, test), ErrorTable.fromCatalog(normalCat, normal)).flatten

      Diagnostic(comparisonTable, errorsTables)
    }
  }

  case class PlanDescription(name: String, env: String, productRatePlanId: ProductRatePlanId, prices: String)

  object PlanDescription {
    def fromPlanDetails(env: String)(productRatePlanId: ProductRatePlanId, plan: MembershipPlan[Status, Tier]): PlanDescription = {
      val prices = plan match {
        case PaidMembershipPlan(_, _, _, _, pricing) =>
          pricing.prices.map(_.pretty).mkString("\n")
        case FreeMembershipPlan(_, _, currencies, _) => currencies.mkString(", ")
      }
      PlanDescription(planName(plan), env, productRatePlanId, prices)
    }
  }

  case class PlanError(name: String, errorMsg: String)

  case class ComparisonTable(rows: Seq[PlanDescription]) {
    def interleave(other: ComparisonTable) =
      ComparisonTable(
        rows.zip(other.rows)
          .flatMap { case p => Seq(p._1, p._2) }
          .sortBy { r => r.name + r.env }
      )
  }

  object ComparisonTable {
    def fromCatalog(catalog: Val[MembershipCatalog], env: String): Option[ComparisonTable] =
      catalog.fold(_ => None, c =>
        ComparisonTable(c.planMap.toSeq.map { case (prpId, plan) =>
          PlanDescription.fromPlanDetails(env)(prpId, plan)
        }).some
      )
  }

  case class ErrorTable(env: String, rows: NonEmptyList[PlanError])

  object ErrorTable {
    def fromCatalog(catalog: Val[MembershipCatalog], env: String): Option[ErrorTable] =
      catalog.fold(errs =>
        ErrorTable(
          env, errs.map { case (tp, msg) => PlanError(planName(tp), msg)}
        ).some, _ => None)
  }

  private def planName(plan: MembershipPlan[Status, Tier]): String = {
    val suffix = plan match {
      case PaidMembershipPlan(_, _, bp, _, _) => s" - ${bp.adverb}"
      case _ => ""
    }
    tierWithStatus(plan.tier, plan.status) + suffix
  }

  private def planName(plan: PlanId): String = {
    val suffix = plan.billingPeriod.fold("")(bp => s" - ${bp.adverb}")
    tierWithStatus(plan.tier, plan.status) + suffix
  }

  private def tierWithStatus(tier: Tier, status: Status) = s"${tier.name} ${status.name}"



  implicit val pricingWrites = new Writes[Pricing] {
    override def writes(p: Pricing): JsValue = Json.obj(
      "yearly" -> p.yearly.amount,
      "monthly" -> p.monthly.amount,
      "saving" -> p.savingInfo
    )
  }

  private def tiers(currency: Currency, catalog: MembershipCatalog): JsValue = JsObject(
    PaidTier.all.map { tier =>
      val details = catalog.findPaid(tier)
      (details.year.pricing.getPrice(currency), details.month.pricing.getPrice(currency), tier)
    }.collect {
      case (Some(y), Some(m), tier) =>
        val pricing = Pricing(y, m)
        tier.toString -> Json.obj(
          "pricing" -> Json.toJson(pricing),
          "benefits" -> Benefits.forTier(tier).map(_.title)
        )
    }
  )

  implicit val catalogWrites = new Writes[MembershipCatalog] {
    override def writes(c: MembershipCatalog) = JsObject(
      Currency.all.map { cur => cur.toString -> tiers(cur, c) }
    )
  }
}
