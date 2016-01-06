package model

import com.gu.membership.model._
import com.gu.salesforce.Tier._
import com.gu.touchpoint.TouchpointBackendConfig.BackendType
import com.gu.zuora.rest
import com.gu.zuora.rest.Readers._
import com.gu.zuora.rest.{ProductCatalog, ProductRatePlan, ProductRatePlanCharge}
import configuration.Config.productFamily
import org.specs2.mutable.Specification
import utils.Resource

import scalaz.syntax.validation._
import scalaz.{NonEmptyList, ValidationNel}

class MembershipCatalogTest extends Specification {
  implicit val backendType = BackendType.Default

  def productRatePlansFromCatalog(fileName: String): Seq[rest.ProductRatePlan] = {
    val json = Resource.getJson(fileName)
    val catalog = parseResponse[ProductCatalog](json).get
    catalog.products.flatMap(_.productRatePlans)
  }

  def addRatePlanCharge(rp: ProductRatePlan): ProductRatePlan = {
    val extraCharge = ProductRatePlanCharge("id","model", None, Nil)
    rp.copy(productRatePlanCharges = extraCharge +: rp.productRatePlanCharges)
  }

  val ratePlanIds = productFamily("DEV")
  val ratePlans = productRatePlansFromCatalog("model/zuora/json/product-catalog-dev.json")

  def changeRatePlans(ids: String*)(f: ProductRatePlan => ProductRatePlan): Seq[ProductRatePlan] =
    ratePlans.map { rp => if (ids.contains(rp.id)) f(rp) else rp }

  def validation(ratePlans: Seq[ProductRatePlan]): ValidationNel[TierPlan, MembershipCatalog] =
    MembershipCatalog.fromZuora(ratePlanIds)(ratePlans).leftMap(_.map(_._1))

  def changePricing(plan: ProductRatePlan)(f: List[String] => List[String]) = {
    val charge = plan.productRatePlanCharges.head
    val newCharge = charge.copy(pricingSummary = f(charge.pricingSummary))
    plan.copy(productRatePlanCharges = Seq(newCharge))
  }

  "MembershipCatalog.fromZuora" should {
    "fail parsing when" >> {
      "an expected ratePlanId is not in the catalog" in {
        val allRatePlansButFriend = ratePlans.filterNot(rp => rp.id == ratePlanIds.friend || rp.id == ratePlanIds.legacy.friend)
        validation(allRatePlansButFriend).shouldEqual(NonEmptyList(
          FriendTierPlan.current,
          FriendTierPlan.legacy
        ).failure)
      }

      "a ratePlan has more than one rate plan charge" in {
        val invalidRatePlans = ratePlans.map { rp =>
          if (rp.id == ratePlanIds.partnerMonthly) addRatePlanCharge(rp) else rp
        }

        validation(invalidRatePlans).shouldEqual(NonEmptyList(
          PaidTierPlan.monthly(Partner, Current)
        ).failure)
      }

      "a ratePlan does not have a GBP price" in {
        val withoutGBP = changeRatePlans(ratePlanIds.partnerMonthly, ratePlanIds.partnerYearly) { plan =>
          changePricing(plan)(_.filterNot(_.startsWith("GBP")))
        }
        validation(withoutGBP).shouldEqual(NonEmptyList(
          PaidTierPlan.monthly(Partner, Current),
          PaidTierPlan.yearly(Partner, Current)
        ).failure)
      }

      "a tier has plan supporting different currencies" in {
        val withAUD = changeRatePlans(ratePlanIds.partnerMonthly) { plan =>
          changePricing(plan)("AUD15" :: _)
        }

        validation(withAUD).shouldEqual(NonEmptyList(
          PaidTierPlan.monthly(Partner, Current),
          PaidTierPlan.yearly(Partner, Current)
        ).failure)
      }

      "a tier plan charge has the wrong model" in {
        val withWrongModel = changeRatePlans(ratePlanIds.partnerMonthly) { plan =>
          val charge = plan.productRatePlanCharges.head
          val newCharge = charge.copy(model = "PerUnit")
          plan.copy(productRatePlanCharges = Seq(newCharge))
        }

        validation(withWrongModel).shouldEqual(NonEmptyList(
          PaidTierPlan.monthly(Partner, Current)
        ).failure)
      }
    }
    "when parsing suceeds" >> {
      val catalog = MembershipCatalog.unsafeFromZuora(ratePlanIds)(ratePlans)

      "parses the friend tierDetail" in {
        catalog.friend.planDetails.productRatePlanId shouldEqual ratePlanIds.friend
        catalog.friend.tier shouldEqual Friend
      }
      "parses the staff tierDetail" in {
        catalog.staff.planDetails.productRatePlanId shouldEqual ratePlanIds.staff
        catalog.staff.tier shouldEqual Staff
      }
      "parses the supporter tier details" in {
        catalog.supporter.monthlyPlanDetails.productRatePlanId shouldEqual ratePlanIds.supporterMonthly
        catalog.supporter.yearlyPlanDetails.productRatePlanId shouldEqual ratePlanIds.supporterYearly
        catalog.supporter.tier shouldEqual Supporter
      }
      "parses the partner tier details" in {
        catalog.partner.monthlyPlanDetails.productRatePlanId shouldEqual ratePlanIds.partnerMonthly
        catalog.partner.yearlyPlanDetails.productRatePlanId shouldEqual ratePlanIds.partnerYearly
        catalog.partner.tier shouldEqual Partner
      }
      "parses the patron tier details" in {
        catalog.patron.monthlyPlanDetails.productRatePlanId shouldEqual ratePlanIds.patronMonthly
        catalog.patron.yearlyPlanDetails.productRatePlanId shouldEqual ratePlanIds.patronYearly
        catalog.patron.tier shouldEqual Patron
      }
    }
  }
}
