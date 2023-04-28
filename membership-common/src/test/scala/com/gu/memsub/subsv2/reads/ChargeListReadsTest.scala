package com.gu.memsub.subsv2.reads

import com.gu.Diff
import Diff._
import com.gu.i18n.Currency._
import com.gu.memsub.Benefit._
import com.gu.memsub.BillingPeriod.Month
import com.gu.memsub.Subscription.{ProductRatePlanChargeId, SubscriptionRatePlanChargeId}
import com.gu.memsub._
import com.gu.memsub.subsv2.{PaidChargeList, _}
import com.gu.memsub.subsv2.reads.ChargeListReads._
import com.softwaremill.diffx.generic.auto.diffForCaseClass
import org.scalatest.flatspec.AnyFlatSpec
import scalaz.{Failure, NonEmptyList, Success, ValidationNel}


class ChargeListReadsTest extends AnyFlatSpec {

  val planChargeMap = Map[ProductRatePlanChargeId, Benefit](
    ProductRatePlanChargeId("weekly") -> Weekly,
    ProductRatePlanChargeId("supporter") -> Supporter,
    ProductRatePlanChargeId("sunday") -> SundayPaper,
    ProductRatePlanChargeId("digipack") -> Digipack
  )

  val supporterCharge = ZuoraCharge.apply(
    productRatePlanChargeId = ProductRatePlanChargeId("supporter"),
    pricing = PricingSummary(Map(
      GBP -> Price(11.99f, GBP)
    )),
    billingPeriod = Some(ZMonth),
    specificBillingPeriod = None,
    model = "FlatFee",
    name = "Supporter",
    `type` = "Recurring",
    endDateCondition = SubscriptionEnd,
    upToPeriods = None,
    upToPeriodsType = None
  )

  val weeklyCharge = ZuoraCharge.apply(
    productRatePlanChargeId = ProductRatePlanChargeId("weekly"),
    pricing = PricingSummary(Map(
      GBP -> Price(30.0f, GBP)
    )),
    billingPeriod = Some(ZMonth),
    specificBillingPeriod = None,
    model = "FlatFee",
    name = "Guardian Weekly Zone A",
    `type` = "Recurring",
    endDateCondition = SubscriptionEnd,
    upToPeriods = None,
    upToPeriodsType = None
  )

  val paperCharge = ZuoraCharge.apply(
    productRatePlanChargeId = ProductRatePlanChargeId("sunday"),
    pricing = PricingSummary(Map(
      GBP -> Price(15.12f, GBP)
    )),
    billingPeriod = Some(ZMonth),
    specificBillingPeriod = None,
    model = "FlatFee",
    name = "Sunday",
    `type` = "Recurring",
    endDateCondition = SubscriptionEnd,
    upToPeriods = None,
    upToPeriodsType = None
  )

  val digipackCharge = ZuoraCharge.apply(
    productRatePlanChargeId = ProductRatePlanChargeId("digipack"),
    pricing = PricingSummary(Map(
      GBP -> Price(11.99f, GBP)
    )),
    billingPeriod = Some(ZMonth),
    specificBillingPeriod = None,
    model = "FlatFee",
    name = "Digipack",
    `type` = "Recurring",
    endDateCondition = SubscriptionEnd,
    upToPeriods = None,
    upToPeriodsType = None
  )

  "product reads" should "read any supporter as any benefit successfully" in {

    val result: ValidationNel[String, Benefit] = implicitly[ChargeReads[Benefit]].read(planChargeMap, supporterCharge)

    val expected: ValidationNel[String, Benefit] = Success(Supporter)

    Diff.assertEquals(expected, result)

  }

  "product reads" should "read any supporter as a supporter successfully" in {

    val result = implicitly[ChargeReads[Supporter.type]].read(planChargeMap, supporterCharge)

    val expected = Success(Supporter)

    Diff.assertEquals(expected, result)

  }

  "product reads" should "not read any supporter as a weekly" in {

    val result = implicitly[ChargeReads[Weekly.type]].read(planChargeMap, supporterCharge)

    val expected = Failure(NonEmptyList("expected class com.gu.memsub.Benefit$Weekly$ but was Supporter (isPhysical? = true)"))

    Diff.assertEquals(expected.leftMap(_.list), result.leftMap(_.list))

  }

  "product reads" should "read any supporter as a member successfully" in {

    val result = implicitly[ChargeReads[MemberTier]].read(planChargeMap, supporterCharge)

    val expected = Success(Supporter)

    Diff.assertEquals(expected, result)

  }

  "product reads" should "not read any supporter as a Free member" in {

    val result = implicitly[ChargeReads[FreeMemberTier]].read(planChargeMap, supporterCharge)

    val expected = Failure(NonEmptyList("expected interface com.gu.memsub.Benefit$FreeMemberTier but was Supporter (isPhysical? = true)"))

    Diff.assertEquals(expected.leftMap(_.list), result.leftMap(_.list))

  }

  "ChargeList reads" should "read single-charge non-paper rate plans as generic PaidCharge type" in {
    val result = implicitly[ChargeListReads[PaidChargeList]].read(planChargeMap, List(weeklyCharge))

    val expected = Success(PaidCharge(Weekly, Month, weeklyCharge.pricing, weeklyCharge.productRatePlanChargeId, weeklyCharge.id))

    Diff.assertEquals(expected, result)

    // Instance type check for extra proof
    assert(!result.exists(_.isInstanceOf[PaperCharges]))
  }

  "ChargeList reads" should "read single-charge paper rate plans as PaperCharges type" in {

    val sundayPrices = Seq((SundayPaper, paperCharge.pricing)).toMap[PaperDay, PricingSummary]

    val result = implicitly[ChargeListReads[PaidChargeList]].read(planChargeMap, List(paperCharge))

    val expected = Success(PaperCharges(dayPrices = sundayPrices, digipack = None))

    Diff.assertEquals(expected, result)

    // Instance type check for extra proof
    assert(result.exists(_.isInstanceOf[PaperCharges]))
  }

  "ChargeList reads" should "not confuse digipack rate plan with paper+digital plan" in {
    val result = implicitly[ChargeListReads[PaidChargeList]].read(planChargeMap, List(digipackCharge))

    val expected = Success(PaidCharge(Digipack, Month, digipackCharge.pricing, digipackCharge.productRatePlanChargeId, digipackCharge.id))

    Diff.assertEquals(expected, result)

    // Instance type check for extra proof
    assert(!result.exists(_.isInstanceOf[PaperCharges]))
  }

}
