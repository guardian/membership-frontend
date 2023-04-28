package com.gu.memsub.subsv2.reads

import com.gu.memsub.{Current, Legacy, Status}
import com.gu.memsub.Subscription.ProductId
import com.gu.memsub.Product._
import com.gu.memsub.Product
import com.gu.memsub.subsv2._
import com.gu.memsub.subsv2.reads.ChargeListReads.ProductIds

import scala.util.Try
import scalaz.ValidationNel
import scalaz.std.list._
import scalaz.syntax.monad._
import scalaz.syntax.nel._
import scalaz.syntax.std.boolean._
import scalaz.syntax.std.option._
import scalaz.syntax.apply.ToApplyOps

trait CatPlanReads[A] {
  def read(p: ProductIds, c: CatalogZuoraPlan): ValidationNel[String, A]
}

object CatPlanReads {

  def findProduct[P <: Product](id: ProductIds => List[ProductId], product: P) = new CatPlanReads[P] {
    override def read(productIds: ProductIds, catalogPlan: CatalogZuoraPlan): ValidationNel[String, P] = {
      val result = id(productIds)
        .contains(catalogPlan.productId)
        .option(product)
        .toSuccess(s"Failed to find ${id(productIds)}".wrapNel)
      result
    }
  }

  implicit val wuk: CatPlanReads[WeeklyZoneA] = findProduct(_.weeklyZoneA.point[List], WeeklyZoneA)
  implicit val w2015: CatPlanReads[WeeklyZoneB] = findProduct(_.weeklyZoneB.point[List], WeeklyZoneB)
  implicit val wrow: CatPlanReads[WeeklyZoneC] = findProduct(_.weeklyZoneC.point[List], WeeklyZoneC)
  implicit val wdom: CatPlanReads[WeeklyDomestic] = findProduct(_.weeklyDomestic.point[List], WeeklyDomestic)
  implicit val wrow2018: CatPlanReads[WeeklyRestOfWorld] = findProduct(_.weeklyRestOfWorld.point[List], WeeklyRestOfWorld)
  implicit val v: CatPlanReads[Voucher] = findProduct(_.voucher.point[List], Voucher)
  implicit val digitalVoucherReads: CatPlanReads[DigitalVoucher] = findProduct(_.digitalVoucher.point[List], DigitalVoucher)
  implicit val d: CatPlanReads[Delivery] = findProduct(_.delivery.point[List], Delivery)
  implicit val y: CatPlanReads[Contribution] = findProduct(_.contributor.point[List], Contribution)
  implicit val s: CatPlanReads[SupporterPlus] = findProduct(_.supporterPlus.point[List], SupporterPlus)
  implicit val z: CatPlanReads[ZDigipack] = findProduct(_.digipack.point[List], Digipack)
  implicit val b: CatPlanReads[Membership] = findProduct(ids => List(ids.friend, ids.supporter, ids.partner, ids.patron, ids.staff), Membership)

  implicit val currentReads: CatPlanReads[Current] = new CatPlanReads[Current] {
    override def read(p: ProductIds, c: CatalogZuoraPlan): ValidationNel[String, Current] =
      (c.status == Status.current).option(Status.current).toSuccess(s"Needed current, got ${c.status}".wrapNel)
  }

  implicit val legacy: CatPlanReads[Legacy] = new CatPlanReads[Legacy] {
    override def read(p: ProductIds, c: CatalogZuoraPlan): ValidationNel[String, Legacy] =
      (c.status == Status.legacy).option(Status.legacy).toSuccess(s"Needed legacy, got ${c.status}".wrapNel)
  }

  implicit val statusReads: CatPlanReads[Status] = new CatPlanReads[Status] {
    override def read(p: ProductIds, c: CatalogZuoraPlan): ValidationNel[String, Status] =
      legacy.read(p, c).map(identity[Status]) orElse currentReads.read(p, c).map(identity[Status])
  }

  implicit def planReads[P <: Product : CatPlanReads, C <: ChargeList : ChargeListReads, S <: Status : CatPlanReads] = new CatPlanReads[CatalogPlan[P, C, S]] {
    override def read(p: ProductIds, c: CatalogZuoraPlan): ValidationNel[String, CatalogPlan[P, C, S]] = (
      implicitly[ChargeListReads[C]].read(c.benefits, c.charges) |@|
      implicitly[CatPlanReads[P]].read(p, c) |@|
      implicitly[CatPlanReads[S]].read(p, c)) { case(charges, product, s) =>
        CatalogPlan(c.id, product, c.name, c.description, Try(c.saving.get.toInt).toOption, charges, s)
      }
  }
}
