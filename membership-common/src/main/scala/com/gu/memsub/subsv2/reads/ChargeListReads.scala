package com.gu.memsub.subsv2.reads
import com.gu.memsub.Benefit._
import com.gu.memsub.BillingPeriod._
import com.gu.memsub.Subscription.{ProductId, ProductRatePlanChargeId}
import com.gu.memsub._
import com.gu.memsub.subsv2._
import com.gu.memsub.subsv2.reads.ChargeListReads.PlanChargeMap
import com.gu.memsub.subsv2.reads.CommonReads.{FailureAggregatingOrElse, TraceableValidation}

import scala.reflect.ClassTag
import scalaz._
import scalaz.syntax.applicative._
import scalaz.syntax.nel._
import scalaz.syntax.std.boolean._
import scalaz.syntax.std.option._
import scalaz.Validation.FlatMap._

/**
  * Try to convert a single ZuoraCharge into some type A
  */
trait ChargeReads[A] {
  def read(cat: PlanChargeMap, charge: ZuoraCharge): ValidationNel[String, A]

  // only read if the result is the given type
  def filter[B <: A : ClassTag]: ChargeReads[B] = {
    val requiredClass = implicitly[ClassTag[B]].runtimeClass
    (cat: PlanChargeMap, charge: ZuoraCharge) => ChargeReads.this.read(cat, charge).flatMap {
      case b: B if requiredClass.isInstance(b) => Success(b)
      case actual => Failure(s"expected $requiredClass but was $actual").toValidationNel
    }
  }
}

/**
  * Try to convert a list of Zuora charges into some type A
  */
trait ChargeListReads[A] {
  def read(cat: PlanChargeMap, charges: List[ZuoraCharge]): ValidationNel[String, A]
}


object ChargeListReads {

  type PlanChargeMap = Map[ProductRatePlanChargeId, Benefit]

  def pure[A](a: A) = new ChargeListReads[A] {
    override def read(cat: PlanChargeMap, charges: List[ZuoraCharge]): ValidationNel[String, A] =
      Validation.s[NonEmptyList[String]](a)
  }

  case class ProductIds(
    weeklyZoneA: ProductId,
    weeklyZoneB: ProductId,
    weeklyZoneC: ProductId,
    weeklyDomestic: ProductId,
    weeklyRestOfWorld: ProductId,
    friend: ProductId,
    supporter: ProductId,
    partner: ProductId,
    patron: ProductId,
    staff: ProductId,
    digipack: ProductId,
    supporterPlus: ProductId,
    voucher: ProductId,
    digitalVoucher: ProductId,
    delivery: ProductId,
    contributor: ProductId
  )

  // shorthand syntax for creating new ChargeListReads
  def apply[A](f: (PlanChargeMap, List[ZuoraCharge]) => ValidationNel[String, A]) = new ChargeListReads[A] {
    override def read(cat: PlanChargeMap, charges: List[ZuoraCharge]): ValidationNel[String, A] = f(cat, charges)
  }

  implicit def readAnyProduct[P <: Benefit : ClassTag]: ChargeReads[P] = new ChargeReads[Benefit] {
    def read(cat: PlanChargeMap, charge: ZuoraCharge): ValidationNel[String, Benefit] =
      cat.get(charge.productRatePlanChargeId).toSuccess(NonEmptyList(s"Could not find product ${charge.name} ${charge.productRatePlanChargeId} in catalog"))
  }.filter[P]

  implicit def anyBpReads[B <: BillingPeriod : ClassTag]: ChargeReads[B] = new ChargeReads[BillingPeriod] {

    def read(cat: PlanChargeMap, charge: ZuoraCharge): ValidationNel[String, BillingPeriod] = {

      ((charge.endDateCondition, charge.billingPeriod) match {
        case (FixedPeriod, Some(ZSpecificWeeks))
          if charge.specificBillingPeriod.exists(numberOfWeeks => numberOfWeeks == 6 || numberOfWeeks == 7) &&
            charge.upToPeriods.contains(1) &&
            charge.upToPeriodsType.contains(BillingPeriods) =>
          Validation.success[String, BillingPeriod](SixWeeks)
        case (FixedPeriod, Some(zPeriod))
          if charge.upToPeriods.contains(1) &&
            charge.upToPeriodsType.contains(BillingPeriods) =>
          zPeriod match {
            case ZYear => Validation.success[String, BillingPeriod](OneYear)
            case ZQuarter => Validation.success[String, BillingPeriod](ThreeMonths)
            case ZTwoYears => Validation.success[String, BillingPeriod](TwoYears)
            case ZThreeYears => Validation.success[String, BillingPeriod](ThreeYears)
            case ZSemiAnnual => Validation.success[String, BillingPeriod](SixMonths)
            case _ => Validation.f[BillingPeriod](s"zuora fixed period was $zPeriod")
          }
        case (SubscriptionEnd, Some(zPeriod)) =>
          zPeriod match {
            case ZMonth => Validation.success[String, BillingPeriod](Month)
            case ZQuarter => Validation.success[String, BillingPeriod](Quarter)
            case ZYear => Validation.success[String, BillingPeriod](Year)
            case ZSemiAnnual => Validation.success[String, BillingPeriod](SixMonthsRecurring)
            case _ => Validation.f[BillingPeriod](s"zuora recurring period was $zPeriod")
          }
        case (OneTime, None) => Validation.success[String, BillingPeriod](OneTimeChargeBillingPeriod) // This represents a one time rate plan charge
        case _ =>
          Validation.f[BillingPeriod](s"period =${charge.billingPeriod} specificBillingPeriod=${charge.specificBillingPeriod} uptoPeriodsType=${charge.upToPeriodsType}, uptoPeriods=${charge.upToPeriods}")
      }).toValidationNel.withTrace("anyBpReads")


    }
  }.filter[B]

  implicit def readPaidCharge[P <: Benefit, BP <: BillingPeriod](implicit product: ChargeReads[P], bp: ChargeReads[BP]): ChargeListReads[PaidCharge[P, BP]] = new ChargeListReads[PaidCharge[P, BP]] {
    def read(cat: PlanChargeMap, charges: List[ZuoraCharge]): ValidationNel[String, PaidCharge[P, BP]] = charges match {
      case charge :: Nil => (product.read(cat, charge) |@| bp.read(cat, charge) |@|
        charge.pricing.prices.exists(_.amount != 0).option(charge.pricing).toSuccess(NonEmptyList("Could not read paid charge: Charge is free")))
        .apply({ case(p, b, pricing) => PaidCharge(p, b, pricing, charge.productRatePlanChargeId, charge.id) })
      case charge :: others => Validation.failureNel(s"Too many charges! I got $charge and $others")
      case Nil => Validation.failureNel(s"No charges found!")
    }
  }

  implicit def readFreeCharge[P <: Benefit](implicit product: ChargeReads[P]): ChargeListReads[FreeCharge[P]] = new ChargeListReads[FreeCharge[P]] {
    def read(cat: PlanChargeMap, charges: List[ZuoraCharge]): ValidationNel[String, FreeCharge[P]] = charges match {
      case charge :: Nil => (product.read(cat, charge) |@| charge.pricing.prices.forall(_.amount == 0).option(charge.pricing)
        .toSuccess(NonEmptyList("Could not read free charge: Charge is paid"))).apply({ case (p, _) => FreeCharge(p, charge.pricing.currencies) })
      case charge :: others => Validation.failureNel(s"Too many charges! I got $charge and $others")
      case Nil => Validation.failureNel(s"No charges found!")
    }
  }

  implicit def readPaidChargeList: ChargeListReads[PaidChargeList] = new ChargeListReads[PaidChargeList] {
    def read(cat: PlanChargeMap, charges: List[ZuoraCharge]): ValidationNel[String, PaidChargeList] = {
      readPaperChargeList.read(cat, charges).map(identity[PaidChargeList]) orElse2
       readPaidCharge[Benefit, BillingPeriod](readAnyProduct, anyBpReads).read(cat, charges)
    }.withTrace("readPaidChargeList")
  }

  implicit def readFreeChargeList: ChargeListReads[FreeChargeList] = new ChargeListReads[FreeChargeList] {
    def read(cat: PlanChargeMap, charges: List[ZuoraCharge]): ValidationNel[String, FreeChargeList] = {
      readFreeCharge[Benefit](readAnyProduct).read(cat, charges).map(identity[FreeChargeList])
    }
  }

  implicit def readChargeList: ChargeListReads[ChargeList] = new ChargeListReads[ChargeList] {
    override def read(cat: PlanChargeMap, charges: List[ZuoraCharge]) = {
      readPaidChargeList.read(cat, charges) orElse2
      readFreeChargeList.read(cat, charges)
    }.withTrace("readChargeList")
  }

  implicit def readPaperChargeList: ChargeListReads[PaperCharges] = new ChargeListReads[PaperCharges] {

    def findDigipack(chargeMap: List[(Benefit, PricingSummary)]): ValidationNel[String, Option[PricingSummary]] =
      chargeMap.collect { case (Digipack, p) => (Digipack, p) } match {
        case Nil => Validation.success[NonEmptyList[String], Option[PricingSummary]](None)
        case n :: Nil => Validation.s[NonEmptyList[String]](Some(n._2))
        case n :: ns => Validation.failureNel("Too many digipacks")
      }

    def getDays(chargeMap: List[(Benefit, PricingSummary)]): ValidationNel[String, Map[PaperDay, PricingSummary]] = {
     val foundDays = chargeMap.collect({ case(d: PaperDay, p) => (d, p) })
      Validation.success(foundDays.toMap)
        .ensure("There are duplicate days".wrapNel)(_.size == foundDays.size)
        .ensure("No days found".wrapNel)(_.nonEmpty)
    }

    override def read(cat: PlanChargeMap, charges: List[ZuoraCharge]): ValidationNel[String, PaperCharges] = {
      val chargeMap = charges.flatMap(c => cat.get(c.productRatePlanChargeId).map(_ -> c.pricing))
      (getDays(chargeMap) |@| findDigipack(chargeMap))(PaperCharges).withTrace("readPaperChargeList")
    }
  }

  implicit def readSingle[B <: Benefit : ChargeReads]: ChargeListReads[ChargeList with SingleBenefit[B]] =
    new ChargeListReads[ChargeList with SingleBenefit[B]] {
    def read(cat: PlanChargeMap, charges: List[ZuoraCharge]): ValidationNel[String, ChargeList with SingleBenefit[B]] = {
      readPaidCharge[B, BillingPeriod].read(cat, charges).map(identity[ChargeList with SingleBenefit[B]]) orElse2
      readFreeCharge[B].read(cat, charges)
    }.withTrace("readSingle")
  }
}
