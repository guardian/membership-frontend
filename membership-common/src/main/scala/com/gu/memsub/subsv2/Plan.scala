package com.gu.memsub.subsv2
import com.gu.i18n.Currency
import Currency.GBP
import com.gu.memsub.Subscription.{ProductId, ProductRatePlanChargeId, ProductRatePlanId, RatePlanId, SubscriptionRatePlanChargeId, Feature => SubsFeature}

import scalaz.NonEmptyList
import com.gu.memsub._
import com.gu.zuora.rest.Feature

import scalaz.syntax.semigroup._
import PricingSummary._
import org.joda.time.LocalDate
import play.api.libs.json._

import scala.language.higherKinds
import BillingPeriod._
import Benefit._


trait ZuoraEnum {
  def id: String
}

object ZuoraEnum {
  def getReads[T <: ZuoraEnum](allValues: Seq[T], errorMessage: String): Reads[T] = new Reads[T] {
    override def reads(json: JsValue): JsResult[T] = json match {
      case JsString(zuoraId) => allValues.find(_.id == zuoraId).map(JsSuccess(_)).getOrElse(JsError(s"$errorMessage: $zuoraId"))
      case v => JsError(s"$errorMessage: $v")
    }
  }
}

sealed trait EndDateCondition extends ZuoraEnum

case object SubscriptionEnd extends EndDateCondition {
  override val id = "Subscription_End"
}

case object FixedPeriod extends EndDateCondition {
  override val id = "Fixed_Period"
}

case object SpecificEndDate extends EndDateCondition {
  override val id = "Specific_End_Date"
}

case object OneTime extends EndDateCondition {
  override val id = "One_Time"
}

object EndDateCondition {
  val values = Seq(SubscriptionEnd, FixedPeriod, SpecificEndDate, OneTime)
  implicit val reads: Reads[EndDateCondition] = ZuoraEnum.getReads(values, "invalid end date condition value")
}

sealed trait ZBillingPeriod extends ZuoraEnum

case object ZYear extends ZBillingPeriod {
  override val id = "Annual"
}

case object ZMonth extends ZBillingPeriod {
  override val id = "Month"
}

case object ZQuarter extends ZBillingPeriod {
  override val id = "Quarter"
}

case object ZSemiAnnual extends ZBillingPeriod {
  override val id = "Semi_Annual"
}

case object ZSpecificMonths extends ZBillingPeriod {
  override val id = "Specific_Months"
}

case object ZWeek extends ZBillingPeriod {
  override val id = "Week"
}

case object ZSpecificWeeks extends ZBillingPeriod {
  override val id = "Specific_Weeks"
}
case object ZTwoYears extends ZBillingPeriod {
  override val id = "Two_Years"
}

case object ZThreeYears extends ZBillingPeriod {
  override val id = "Three_Years"
}

object ZBillingPeriod {
  val values = Seq(ZYear, ZTwoYears, ZThreeYears, ZMonth, ZQuarter, ZSemiAnnual, ZSpecificMonths, ZWeek, ZSpecificWeeks)
  implicit val reads: Reads[ZBillingPeriod] = ZuoraEnum.getReads(values, "invalid billing period value")
}

sealed trait UpToPeriodsType extends ZuoraEnum

case object BillingPeriods extends UpToPeriodsType {
  override val id = "Billing_Periods"
}

case object Days extends UpToPeriodsType {
  override val id = "Days"
}

case object Weeks extends UpToPeriodsType {
  override val id = "Weeks"
}

case object Months extends UpToPeriodsType {
  override val id = "Months"
}

case object Years extends UpToPeriodsType {
  override val id = "Years"
}

object UpToPeriodsType {
  val values = Seq(BillingPeriods, Days, Weeks, Months, Years)
  implicit val reads: Reads[UpToPeriodsType] = ZuoraEnum.getReads(values, "invalid up to periods type value")
}

/**
  * Low level model of a Zuora rate plan charge
  */
case class ZuoraCharge(
  id: SubscriptionRatePlanChargeId,
  productRatePlanChargeId: ProductRatePlanChargeId,
  pricing: PricingSummary,
  billingPeriod: Option[ZBillingPeriod],
  specificBillingPeriod: Option[Int] = None,
  model: String,
  name: String,
  `type`: String,
  endDateCondition: EndDateCondition,
  upToPeriods: Option[Int],
  upToPeriodsType: Option[UpToPeriodsType]
)

object ZuoraCharge {
  def apply(
    productRatePlanChargeId: ProductRatePlanChargeId,
    pricing: PricingSummary,
    billingPeriod: Option[ZBillingPeriod],
    specificBillingPeriod: Option[Int],
    model: String,
    name: String,
    `type`: String,
    endDateCondition: EndDateCondition,
    upToPeriods: Option[Int],
    upToPeriodsType: Option[UpToPeriodsType]
  ): ZuoraCharge = ZuoraCharge(
      SubscriptionRatePlanChargeId(""),
      productRatePlanChargeId,
      pricing,
      billingPeriod,
      specificBillingPeriod,
      model,
      name,
      `type`,
      endDateCondition,
      upToPeriods,
      upToPeriodsType
  )
}

/**
  * Low level model of a rate plan, as it appears
  * on a subscription in Zuora
  */
case class SubscriptionZuoraPlan(
  id: RatePlanId,
  productRatePlanId: ProductRatePlanId,
  productName: String,
  features: List[Feature],
  chargedThroughDate: Option[LocalDate],
  charges: NonEmptyList[ZuoraCharge],
  start: LocalDate,
  end: LocalDate
) {
  def price = charges.list.toList.flatMap { charge =>
    charge.pricing.prices.map(_.amount)
  }.sum
}

/**
  * Low level model of a product rate plan,
  * as it appears in the Zuora product catalog
  */
case class CatalogZuoraPlan(
  id: ProductRatePlanId,
  name: String,
  description: String,
  productId: ProductId,
  saving: Option[String],
  charges: List[ZuoraCharge],
  benefits: Map[ProductRatePlanChargeId, Benefit],
  status: Status,
  frontendId: Option[FrontendId],
  private val productTypeOption: Option[String],
) {
  lazy val productType = productTypeOption.getOrElse(throw new RuntimeException("Product type is undefined for plan: " + name))
}

sealed trait FrontendId {
  def name: String
}
object FrontendId {

  case object OneYear extends FrontendId { val name = "OneYear" }
  case object ThreeMonths extends FrontendId { val name = "ThreeMonths" }
  case object Monthly extends FrontendId { val name = "Monthly" }
  case object Quarterly extends FrontendId { val name = "Quarterly" }
  case object Yearly extends FrontendId { val name = "Yearly" }
  case object Introductory extends FrontendId { val name = "Introductory" }
  case object Free extends FrontendId { val name = "Free" }
  case object SixWeeks extends FrontendId { val name = "SixWeeks" }

  val all = List(OneYear, ThreeMonths, Monthly, Quarterly, Yearly, Introductory, Free, SixWeeks)

  def get(jsonString: String): Option[FrontendId] =
    all.find(_.name == jsonString)

}

object CatalogPlan {

  type Member = CatalogPlan[Product.Membership, ChargeList with SingleBenefit[MemberTier], Current]
  type FreeMember = CatalogPlan[Product.Membership, FreeCharge[FreeMemberTier], Current]
  type Friend = CatalogPlan[Product.Membership, FreeCharge[Friend.type], Current]
  type Staff = CatalogPlan[Product.Membership, FreeCharge[Staff.type], Current]

  type PaidMember[+B <: BillingPeriod] = CatalogPlan[Product.Membership, PaidCharge[PaidMemberTier, B], Current]
  type Supporter[+B <: BillingPeriod] = CatalogPlan[Product.Membership, PaidCharge[Supporter.type, B], Current]
  type Partner[+B <: BillingPeriod] = CatalogPlan[Product.Membership, PaidCharge[Partner.type, B], Current]
  type Patron[+B <: BillingPeriod] = CatalogPlan[Product.Membership, PaidCharge[Patron.type, B], Current]

  type Contributor = CatalogPlan[Product.Contribution, PaidCharge[Contributor.type, Month.type], Current]

  type Digipack[+B <: BillingPeriod] = CatalogPlan[Product.ZDigipack, PaidCharge[Digipack.type, B], Current]
  type SupporterPlus[+B <: BillingPeriod] = CatalogPlan[Product.SupporterPlus, SupporterPlusCharges, Current]
  type Delivery = CatalogPlan[Product.Delivery, PaperCharges, Current]
  type Voucher = CatalogPlan[Product.Voucher, PaperCharges, Current]
  type DigitalVoucher = CatalogPlan[Product.DigitalVoucher, PaperCharges, Current]
  type AnyPlan = CatalogPlan[Product, ChargeList, Current]

  type Paid = CatalogPlan[Product, PaidChargeList, Current]
  type Free = CatalogPlan[Product, FreeChargeList, Current]

  type WeeklyZoneA[+B <: BillingPeriod] = CatalogPlan[Product.WeeklyZoneA, PaidCharge[Weekly.type, B], Current]
  type WeeklyZoneB[+B <: BillingPeriod] = CatalogPlan[Product.WeeklyZoneB, PaidCharge[Weekly.type, B], Current]
  type WeeklyZoneC[+B <: BillingPeriod] = CatalogPlan[Product.WeeklyZoneC, PaidCharge[Weekly.type, B], Current]
  type WeeklyDomestic[+B <: BillingPeriod] = CatalogPlan[Product.WeeklyDomestic, PaidCharge[Weekly.type, B], Current]
  type WeeklyRestOfWorld[+B <: BillingPeriod] = CatalogPlan[Product.WeeklyRestOfWorld, PaidCharge[Weekly.type, B], Current]

  type Paper = CatalogPlan[Product.Paper, PaidChargeList, Current]
  type ContentSubscription = CatalogPlan[Product.ContentSubscription, PaidChargeList, Current]

  type RecurringContentSubscription[+B <: BillingPeriod] = CatalogPlan[Product.ContentSubscription, PaidCharge[Benefit, B], Current]
  type RecurringPlan[+B <: BillingPeriod] = CatalogPlan[Product, PaidCharge[Benefit, B], Current]
}


case class PlansWithIntroductory[+B](plans: List[B], associations: List[(B, B)])

case class PaidMembershipPlans[+B <: Benefit](month: CatalogPlan[Product.Membership, PaidCharge[B, Month.type], Current], year: CatalogPlan[Product.Membership, PaidCharge[B, Year.type], Current]) {
  lazy val plans = List(month, year)
}

case class DigipackPlans(month: CatalogPlan.Digipack[Month.type], quarter: CatalogPlan.Digipack[Quarter.type], year: CatalogPlan.Digipack[Year.type]) {
  lazy val plans = List(month, quarter, year)
}

case class SupporterPlusPlans(month: CatalogPlan.SupporterPlus[Month.type], year: CatalogPlan.SupporterPlus[Year.type]) {
  lazy val plans = List(month, year)
}

case class WeeklyZoneBPlans(quarter: CatalogPlan.WeeklyZoneB[Quarter.type], year: CatalogPlan.WeeklyZoneB[Year.type], oneYear: CatalogPlan.WeeklyZoneB[OneYear.type]) {
  lazy val plans = List(quarter, year, oneYear)
  val plansWithAssociations = PlansWithIntroductory(plans, List.empty)
}
case class WeeklyZoneAPlans(sixWeeks: CatalogPlan.WeeklyZoneA[SixWeeks.type], quarter: CatalogPlan.WeeklyZoneA[Quarter.type], year: CatalogPlan.WeeklyZoneA[Year.type], oneYear: CatalogPlan.WeeklyZoneA[OneYear.type]) {
  val plans = List(sixWeeks, quarter, year, oneYear)
  val associations = List(sixWeeks -> quarter)
  val plansWithAssociations = PlansWithIntroductory(plans, associations)
}
case class WeeklyZoneCPlans(sixWeeks: CatalogPlan.WeeklyZoneC[SixWeeks.type], quarter: CatalogPlan.WeeklyZoneC[Quarter.type], year: CatalogPlan.WeeklyZoneC[Year.type]) {
  lazy val plans = List(sixWeeks, quarter, year)
  val associations = List(sixWeeks -> quarter)
  val plansWithAssociations = PlansWithIntroductory(plans, associations)
}
case class WeeklyDomesticPlans(
  sixWeeks: CatalogPlan.WeeklyDomestic[SixWeeks.type],
  quarter: CatalogPlan.WeeklyDomestic[Quarter.type],
  year: CatalogPlan.WeeklyDomestic[Year.type],
  month: CatalogPlan.WeeklyDomestic[Month.type],
  oneYear: CatalogPlan.WeeklyDomestic[OneYear.type],
  threeMonths: CatalogPlan.WeeklyDomestic[ThreeMonths.type]
) {
  lazy val plans = List(sixWeeks, quarter, year, month, oneYear, threeMonths)
  val associations = List(sixWeeks -> quarter)
  val plansWithAssociations = PlansWithIntroductory(plans, associations)
}

case class WeeklyRestOfWorldPlans(
  sixWeeks: CatalogPlan.WeeklyRestOfWorld[SixWeeks.type],
  quarter: CatalogPlan.WeeklyRestOfWorld[Quarter.type],
  year: CatalogPlan.WeeklyRestOfWorld[Year.type],
  month: CatalogPlan.WeeklyRestOfWorld[Month.type],
  oneYear: CatalogPlan.WeeklyRestOfWorld[OneYear.type],
  threeMonths: CatalogPlan.WeeklyRestOfWorld[ThreeMonths.type]
) {
  lazy val plans = List(sixWeeks, quarter, year, month, oneYear, threeMonths)
  val associations = List(sixWeeks -> quarter)
  val plansWithAssociations = PlansWithIntroductory(plans, associations)
}

case class WeeklyPlans(
    zoneA: WeeklyZoneAPlans,
    zoneB: WeeklyZoneBPlans,
    zoneC: WeeklyZoneCPlans,
    domestic: WeeklyDomesticPlans,
    restOfWorld: WeeklyRestOfWorldPlans
) {
  val plans = List(zoneA.plans, zoneB.plans, zoneC.plans, domestic.plans, restOfWorld.plans)
}

case class Catalog(
  friend: CatalogPlan.Friend,
  staff: CatalogPlan.Staff,
  supporter: PaidMembershipPlans[Supporter.type],
  partner: PaidMembershipPlans[Partner.type],
  patron: PaidMembershipPlans[Patron.type],
  digipack: DigipackPlans,
  supporterPlus: SupporterPlusPlans,
  contributor: CatalogPlan.Contributor,
  voucher: NonEmptyList[CatalogPlan.Voucher],
  digitalVoucher: NonEmptyList[CatalogPlan.DigitalVoucher],
  delivery: NonEmptyList[CatalogPlan.Delivery],
  weekly: WeeklyPlans,
  map: Map[ProductRatePlanId, CatalogZuoraPlan]
) {

  lazy val paid: Seq[CatalogPlan.Paid] =
    supporter.plans ++ partner.plans ++ patron.plans ++ allSubs.flatten

  lazy val allSubs: List[List[CatalogPlan.Paid]] =
    List(digipack.plans, supporterPlus.plans, voucher.list.toList, digitalVoucher.list.toList, delivery.list.toList) ++ weekly.plans

  lazy val allMembership: List[CatalogPlan[Product.Membership, ChargeList with SingleBenefit[MemberTier], Current]] =
    List(friend, staff) ++ supporter.plans ++ partner.plans ++ patron.plans
}

/**
  * A higher level representation of a number of Zuora rate plan charges
  */
sealed trait ChargeList {
  def benefits: NonEmptyList[Benefit]
  def currencies: Set[Currency]
}

sealed trait FreeChargeList extends ChargeList {
  def currencies: Set[Currency]
}

sealed trait PaidChargeList extends ChargeList {
  def gbpPrice = price.getPrice(GBP).getOrElse(throw new Exception("No GBP price"))
  def currencies = price.currencies
  def billingPeriod: BillingPeriod
  def price: PricingSummary
  def subRatePlanChargeId: SubscriptionRatePlanChargeId
}

/**
  * Generic version of single free / paid charge
  * This is to allow exhaustive matches on tier in membership
  * i.e. the common ancestor type of Friend / Supporter will be Plan[ChargeList with SingleBenefit[MemberTier]]
  * as opposed to just Plan[ChargeList] which isn't typed to only contain member tiers
  */
sealed trait SingleBenefit[+B <: Benefit] {
  def benefit: B
}

/**
  * So this is a charge "list" that must contain exactly one free charge
  * like if you're a friend on membership
  */
case class FreeCharge[+B <: Benefit](benefit: B, currencies: Set[Currency]) extends FreeChargeList with SingleBenefit[B] {
  def benefits = NonEmptyList(benefit)
}

/**
  * Same as above but we must have exactly one paid charge, giving us exactly one benefit
  * This is used for supporter, partner, patron and digital pack subs
  */
case class PaidCharge[+B <: Benefit, +BP <: BillingPeriod](benefit: B, billingPeriod: BP, price: PricingSummary, chargeId: ProductRatePlanChargeId, subRatePlanChargeId: SubscriptionRatePlanChargeId) extends PaidChargeList with SingleBenefit[B]  {
  def benefits = NonEmptyList(benefit)
}

/**
  * Paper plans will have lots of rate plan charges, but the general structure of them is that they'll
  * give you the paper on a bunch of days, and if you're on a plus plan you'll have a digipack
  */
case class PaperCharges(dayPrices: Map[PaperDay, PricingSummary], digipack: Option[PricingSummary]) extends PaidChargeList {
  def benefits = NonEmptyList.fromSeq[Benefit](dayPrices.keys.head, dayPrices.keys.tail.toSeq ++ digipack.map(_ => Digipack))
  def price: PricingSummary = (dayPrices.values.toSeq ++ digipack.toSeq).reduce(_ |+| _)
  override def billingPeriod: BillingPeriod = BillingPeriod.Month
  def chargedDays = dayPrices.filterNot(_._2.isFree).keySet // Non-subscribed-to papers are priced as Zero on multi-day subs
  val subRatePlanChargeId = SubscriptionRatePlanChargeId("")
}

case class SupporterPlusCharges(billingPeriod: BillingPeriod, pricingSummaries: List[PricingSummary]) extends PaidChargeList {

  val subRatePlanChargeId = SubscriptionRatePlanChargeId("")
  override def price: PricingSummary = pricingSummaries.reduce(_ |+| _)
  override def benefits: NonEmptyList[Benefit] = NonEmptyList(SupporterPlus)
}

/**
  * This is the higher level model of a zuora rate plan,
  * This particular trait is stuff common to both catalog and subscription plans
  */
sealed trait Plan[+P <: Product, +C <: ChargeList]  {
  def name: String
  def description: String
  def charges: C
  def product: P
}

// a plan as it appears on a zuora subscription, as opposed to in the product catalog
sealed trait SubscriptionPlan[+P <: Product, +C <: ChargeList] extends Plan[P, C] {
  def productRatePlanId: ProductRatePlanId
  def productName: String
  def productType: String
  override def product: P
  def isPaid: Boolean
  def id: RatePlanId
  def charges: C
  def start: LocalDate
  def end: LocalDate
}

/**
  * we split subscription plans as to whether they're paid or free
  * because some fields are specific to paid plans - i.e. charged through
  */
case class PaidSubscriptionPlan[+P <: Product, +C <: PaidChargeList](
  id: RatePlanId,
  productRatePlanId: ProductRatePlanId,
  name: String,
  description: String,
  productName: String,
  productType: String,
  product: P,
  features: List[SubsFeature],
  charges: C,
  chargedThrough: Option[LocalDate], // this is None if the sub hasn't been billed yet (on a free trial)
  start: LocalDate,
  end: LocalDate
) extends SubscriptionPlan[P, C] {      // or if you have been billed it is the date at which you'll next be billed
  val isPaid = true
}
case class FreeSubscriptionPlan[+P <: Product, +C <: FreeChargeList](
  id: RatePlanId,
  productRatePlanId: ProductRatePlanId,
  name: String,
  description: String,
  productName: String,
  productType: String,
  product: P,
  charges: C,
  start: LocalDate,
  end: LocalDate
) extends SubscriptionPlan[P, C] {
  val isPaid = false
}

/**
  * So this is the higher level model of a zuora product rate plan
  * we don't need to split into paid / free catalog plans as the fields are the same
  */
case class CatalogPlan[+P <: Product, +C <: ChargeList, +S <: Status](
  id: ProductRatePlanId,
  product: P,
  name: String,
  description: String,
  saving: Option[Int],
  charges: C,
  s: S
) extends Plan[P, C] {
  lazy val slug: String =
    s"${product.name}-$name".replace(" ", "").toLowerCase
}

/**
  * So the benefit of all these type parameters on the higher level models
  * is that you can uniquely identify a particular plan by its type signature
  * and if you can do that then you can pass your super specific (or more generic)
  * plan into subscription service to find the subscription of your dreams
  */
object SubscriptionPlan {
  type AnyPlan = SubscriptionPlan[Product, ChargeList]
  type Paid = PaidSubscriptionPlan[Product, PaidChargeList]
  type Free = FreeSubscriptionPlan[Product, FreeChargeList]

  type Member = SubscriptionPlan[Product.Membership, ChargeList with SingleBenefit[MemberTier]]
  type PaidMember = PaidSubscriptionPlan[Product.Membership, PaidCharge[Benefit.PaidMemberTier, BillingPeriod]]
  type FreeMember = FreeSubscriptionPlan[Product.Membership, FreeCharge[Benefit.FreeMemberTier]]

  type Staff = FreeSubscriptionPlan[Product.Membership, FreeCharge[Benefit.Staff.type]]
  type Friend = FreeSubscriptionPlan[Product.Membership, FreeCharge[Benefit.Friend.type]]
  type Supporter = PaidSubscriptionPlan[Product.Membership, PaidCharge[Benefit.Supporter.type, BillingPeriod]]
  type Partner = PaidSubscriptionPlan[Product.Membership, PaidCharge[Benefit.Partner.type, BillingPeriod]]
  type Patron = PaidSubscriptionPlan[Product.Membership, PaidCharge[Benefit.Patron.type, BillingPeriod]]

  type ContentSubscription = PaidSubscriptionPlan[Product.ContentSubscription, PaidChargeList]
  type Digipack = PaidSubscriptionPlan[Product.ZDigipack, PaidCharge[Benefit.Digipack.type, BillingPeriod]]
  type SupporterPlus = PaidSubscriptionPlan[Product.SupporterPlus, PaidCharge[Benefit.SupporterPlus.type, BillingPeriod]]
  type Delivery = PaidSubscriptionPlan[Product.Delivery, PaperCharges]
  type Voucher = PaidSubscriptionPlan[Product.Voucher, PaperCharges]
  type DigitalVoucher = PaidSubscriptionPlan[Product.DigitalVoucher, PaperCharges]
  type DailyPaper = PaidSubscriptionPlan[Product.Paper, PaperCharges]
  type PaperPlan = PaidSubscriptionPlan[Product.Paper, PaidChargeList]

  type WeeklyZoneA = PaidSubscriptionPlan[Product.WeeklyZoneA, PaidCharge[Weekly.type, BillingPeriod]]
  type WeeklyZoneB = PaidSubscriptionPlan[Product.WeeklyZoneB, PaidCharge[Weekly.type, BillingPeriod]]
  type WeeklyPlan = PaidSubscriptionPlan[Product.Weekly, PaidCharge[Weekly.type, BillingPeriod]]

  type Contributor = PaidSubscriptionPlan[Product.Contribution, PaidCharge[Benefit.Contributor.type, BillingPeriod]]
}

