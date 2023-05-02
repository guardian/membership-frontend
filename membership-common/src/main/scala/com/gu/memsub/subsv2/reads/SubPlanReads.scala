package com.gu.memsub.subsv2.reads
import com.gu.memsub.Product._
import com.gu.memsub.Product
import com.gu.memsub.Subscription.ProductId
import com.gu.memsub.subsv2._
import com.gu.memsub.subsv2.reads.ChargeListReads.{ProductIds, _}
import com.gu.memsub.subsv2.reads.CommonReads._
import com.gu.memsub.{Benefit, BillingPeriod}

import scalaz.Validation.FlatMap._
import scalaz.std.list._
import scalaz.syntax.monad._
import scalaz.syntax.nel._
import scalaz.syntax.std.boolean._
import scalaz.syntax.std.option._
import scalaz.{Validation, ValidationNel, \/}
import scalaz.syntax.apply.ToApplyOps

/**
  * Convert a catalog zuora plan and a subscription zuora plan into some type A
  * Between them, the catalog zuora plan & sub zuora plan have enough info to construct a Plan
  */
trait SubPlanReads[A] {
  def read(p: ProductIds, z: SubscriptionZuoraPlan, c: CatalogZuoraPlan): ValidationNel[String, A]
}

object SubPlanReads {

  def findProduct[P <: Product](id: ProductIds => List[ProductId], product: P) = new SubPlanReads[P] {

    override def read(p: ProductIds, z: SubscriptionZuoraPlan, c: CatalogZuoraPlan): ValidationNel[String, P] = {
      id(p).contains(c.productId).option(product).toSuccess(s"couldn't read a $product: as the product id is ${c.productId} but we need one of $p".wrapNel)
    }
  }

  implicit val voucherReads: SubPlanReads[Voucher] = findProduct(_.voucher.point[List], Voucher)
  implicit val digitalVoucherReads: SubPlanReads[DigitalVoucher] = findProduct(_.digitalVoucher.point[List], DigitalVoucher)
  implicit val deliveryReads: SubPlanReads[Delivery] = findProduct(_.delivery.point[List], Delivery)
  implicit val zDigipackReads: SubPlanReads[ZDigipack] = findProduct(_.digipack.point[List], Digipack)
  implicit val supporterPlusReads: SubPlanReads[SupporterPlus] = findProduct(_.supporterPlus.point[List], SupporterPlus)
  implicit val membershipReads: SubPlanReads[Membership] = findProduct(ids => List(ids.friend, ids.supporter, ids.partner, ids.patron, ids.staff), Membership)
  implicit val contributionReads: SubPlanReads[Contribution] = findProduct(_.contributor.point[List], Contribution)
  implicit val weeklyZoneAReads: SubPlanReads[WeeklyZoneA] = findProduct(_.weeklyZoneA.point[List], WeeklyZoneA)
  implicit val weeklyZoneBReads: SubPlanReads[WeeklyZoneB] = findProduct(_.weeklyZoneB.point[List], WeeklyZoneB)
  implicit val weeklyZoneCReads: SubPlanReads[WeeklyZoneC] = findProduct(_.weeklyZoneC.point[List], WeeklyZoneC)
  implicit val weeklyDomesticReads: SubPlanReads[WeeklyDomestic] = findProduct(_.weeklyDomestic.point[List], WeeklyDomestic)
  implicit val weeklyRestOfWorldReads: SubPlanReads[WeeklyRestOfWorld] = findProduct(_.weeklyRestOfWorld.point[List], WeeklyRestOfWorld)

  implicit val productReads = new SubPlanReads[Product] {
    override def read(p: ProductIds, z: SubscriptionZuoraPlan, c: CatalogZuoraPlan): ValidationNel[String, Product] = (
      contentSubscriptionReads.read(p, z, c) orElse2
      membershipReads.read(p, z, c) orElse2
      contributionReads.read(p, z, c)).withTrace("productReads")
  }

  implicit val contentSubscriptionReads: SubPlanReads[ContentSubscription] = new SubPlanReads[ContentSubscription] {
    override def read(p: ProductIds, z: SubscriptionZuoraPlan, c: CatalogZuoraPlan): ValidationNel[String, ContentSubscription] = (
      paperReads.read(p, z, c) orElse2
        zDigipackReads.read(p, z, c)) orElse2
        supporterPlusReads.read(p, z, c).withTrace("contentSubscriptionReads")
  }

  implicit val paperReads = new SubPlanReads[Paper] {
    override def read(p: ProductIds, z: SubscriptionZuoraPlan, c: CatalogZuoraPlan): ValidationNel[String, Paper] = (
      voucherReads.read(p, z, c).map(identity[Paper]) orElse2
      digitalVoucherReads.read(p, z, c) orElse2
      deliveryReads.read(p, z, c) orElse2
      weeklyZoneAReads.read(p, z, c) orElse2
      weeklyZoneBReads.read(p, z, c) orElse2
      weeklyZoneCReads.read(p, z, c)) orElse2
      weeklyDomesticReads.read(p, z, c) orElse2
      weeklyRestOfWorldReads.read(p, z, c).withTrace("paperReads")
  }


  implicit def paidPlanReads[P <: Product, C <: PaidChargeList](implicit productReads: SubPlanReads[P], chargeListReads: ChargeListReads[C])  = new SubPlanReads[PaidSubscriptionPlan[P, C]] {
    override def read(p: ProductIds, z: SubscriptionZuoraPlan, c: CatalogZuoraPlan): ValidationNel[String, PaidSubscriptionPlan[P, C]] =
      (chargeListReads.read(c.benefits, z.charges.list.toList) |@| productReads.read(p, z, c)) { case(charges, product) =>
        val highLevelFeatures = z.features.map(com.gu.memsub.Subscription.Feature.fromRest)
        PaidSubscriptionPlan(z.id, c.id, c.name, c.description, z.productName, c.productType, product, highLevelFeatures, charges, z.chargedThroughDate, z.start, z.end)
      }.withTrace("paidPlanReads")
  }

  implicit def freePlanReads[P <: Product, C <: FreeChargeList](implicit productReads: SubPlanReads[P], chargeListReads: ChargeListReads[C]) = new SubPlanReads[FreeSubscriptionPlan[P, C]] {
    override def read(ids: ProductIds, subZuoraPlan: SubscriptionZuoraPlan, catZuoraPlan: CatalogZuoraPlan): ValidationNel[String, FreeSubscriptionPlan[P, C]] =
      (chargeListReads.read(catZuoraPlan.benefits, subZuoraPlan.charges.list.toList) |@| productReads.read(ids, subZuoraPlan, catZuoraPlan)) { case(charges, product) =>
        FreeSubscriptionPlan(subZuoraPlan.id, catZuoraPlan.id, catZuoraPlan.name, catZuoraPlan.description, subZuoraPlan.productName, catZuoraPlan.productType, product, charges, subZuoraPlan.start, subZuoraPlan.end)
      }.withTrace("freePlanReads")
  }

  implicit def planReads[P <: Product : SubPlanReads, B <: Benefit : ChargeReads]: SubPlanReads[SubscriptionPlan[P, ChargeList with SingleBenefit[B]]] =
    (p: ProductIds, z: SubscriptionZuoraPlan, c: CatalogZuoraPlan) => {
      (
        ChargeListReads.readPaidCharge[B, BillingPeriod].read(c.benefits, z.charges.list.toList).map(\/.r[FreeCharge[B]].apply) orElse
          ChargeListReads.readFreeCharge[B].read(c.benefits, z.charges.list.toList).map(\/.left)
        ).flatMap(_.fold(
        free => freePlanReads(implicitly[SubPlanReads[P]], ChargeListReads.pure(free)).read(p, z, c).map(identity[SubscriptionPlan[P, ChargeList with SingleBenefit[B]]]),
        paid => paidPlanReads(implicitly[SubPlanReads[P]], ChargeListReads.pure(paid)).read(p, z, c).map(identity[SubscriptionPlan[P, ChargeList with SingleBenefit[B]]])
      ))
    }.withTrace("planReads")

  implicit def anyPlanReads[P <: Product](implicit productReads: SubPlanReads[P], chargeListReads: ChargeListReads[ChargeList]): SubPlanReads[SubscriptionPlan[P,ChargeList]] =
    (ids: ProductIds, subZuoraPlan: SubscriptionZuoraPlan, catZuoraPlan: CatalogZuoraPlan) => {
      (chargeListReads.read(catZuoraPlan.benefits, subZuoraPlan.charges.list.toList) |@| productReads.read(ids, subZuoraPlan, catZuoraPlan)) {
        case (charges: ChargeList, product: P) =>
          (charges match {
            case freeChargeList: FreeChargeList =>
              FreeSubscriptionPlan(subZuoraPlan.id, catZuoraPlan.id, catZuoraPlan.name, catZuoraPlan.description, subZuoraPlan.productName, catZuoraPlan.productType, product, freeChargeList, subZuoraPlan.start, subZuoraPlan.end)
            case paidChargeList: PaidChargeList =>
              val highLevelFeatures = subZuoraPlan.features.map(com.gu.memsub.Subscription.Feature.fromRest)
              PaidSubscriptionPlan(subZuoraPlan.id, catZuoraPlan.id, catZuoraPlan.name, catZuoraPlan.description, subZuoraPlan.productName, catZuoraPlan.productType, product, highLevelFeatures, paidChargeList, subZuoraPlan.chargedThroughDate, subZuoraPlan.start, subZuoraPlan.end)
          }): SubscriptionPlan[P, ChargeList]
      }.withTrace("anyPlanReads")
    }
}
