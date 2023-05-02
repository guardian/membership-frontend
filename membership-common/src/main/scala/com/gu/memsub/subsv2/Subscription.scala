package com.gu.memsub.subsv2

import com.github.nscala_time.time.Imports._
import com.gu.memsub
import com.gu.memsub.Benefit._
import com.gu.memsub.BillingPeriod.{OneTimeChargeBillingPeriod, OneYear, ThreeMonths}
import com.gu.memsub.promo.PromoCode
import com.gu.memsub.subsv2.SubscriptionPlan._
import com.gu.memsub.subsv2.services.Sequence
import com.gu.memsub.{BillingPeriod, Product}
import org.joda.time.{DateTime, LocalDate}

import scala.language.higherKinds
import scala.reflect.ClassTag
import scalaz.syntax.all._
import scalaz.{NonEmptyList, Validation, \/}

case class CovariantNonEmptyList[+T](head: T, tail: List[T]) {
  val list = head :: tail
}

case class Subscription[+P <: SubscriptionPlan.AnyPlan](
  id: memsub.Subscription.Id,
  name: memsub.Subscription.Name,
  accountId: memsub.Subscription.AccountId,
  startDate: LocalDate,
  acceptanceDate: LocalDate,
  termStartDate: LocalDate,
  termEndDate: LocalDate,
  casActivationDate: Option[DateTime],
  promoCode: Option[PromoCode],
  isCancelled: Boolean,
  hasPendingFreePlan: Boolean,
  plans: CovariantNonEmptyList[P],
  readerType: ReaderType,
  gifteeIdentityId: Option[String],
  autoRenew: Boolean
) {

  private def isPaid[P <: SubscriptionPlan.AnyPlan](plan: P) = plan.charges match {
    case _: PaidChargeList => true
    case _ => false
  }

  val firstPaymentDate = {
    val paidPlans = plans.list.filter(isPaid)
    (acceptanceDate :: paidPlans.map(_.start)).toList.min
  }

  lazy val plan: P = {
    GetCurrentPlans(this, LocalDate.now).fold(error => throw new RuntimeException(error), _.head)
  }

  // can we make it optional to specify A and B?
  def as[A <: Product, B <: ChargeList, SP <: SubscriptionPlan[A,B]](implicit a: ClassTag[A], b: ClassTag[B]): Option[Subscription[SP]] = {
    if (a.runtimeClass.isInstance(plans.head.product) && b.runtimeClass.isInstance(plans.head.charges))
      Some(asInstanceOf[Subscription[SP]])
    else
      None
  }

  def asDelivery = as[Product.Delivery, PaperCharges, SubscriptionPlan.Delivery]
  def asVoucher = as[Product.Voucher, PaperCharges, SubscriptionPlan.Voucher]
  def asWeekly = as[Product.Weekly, PaidCharge[Weekly.type, BillingPeriod]/* TODO should check the benefit and billing period*/, SubscriptionPlan.WeeklyPlan]
  def asDigipack = as[Product.ZDigipack, PaidChargeList, SubscriptionPlan.Digipack]
  def asContribution = as[Product.Contribution, PaidChargeList, SubscriptionPlan.Contributor]
  def asMembership = as[Product.Membership, ChargeList, SubscriptionPlan.Member]
}

/*
this goes through all the plan objects and only returns the ones that are current.
This could include more than one of the same type especially if it's the changeover date.
It then sorts them so the "best" one is first in the list.  Best just means more expensive,
so this code could be an area of disaster in future.
If we are comparing two Free plans (possible because there is a legacy Friend plan in Zuora) we need to work
with the newest plan for upgrade and cancel scenarios, so in this case the most recent start date wins.
 */
object GetCurrentPlans {

  /*- negative if x < y
 *  - positive if x > y
 *  - zero otherwise (if x == y)*/
  val planGoodnessOrder = new scala.Ordering[SubscriptionPlan.AnyPlan] {
    val lt = -1; val eq = 0; val gt = 1
    override def compare(x: AnyPlan, y: AnyPlan): Int = {
      (x, y) match {
        case (_: PaidSubscriptionPlan[_, _], _: FreeSubscriptionPlan[_, _]) => gt
        case (_: FreeSubscriptionPlan[_, _], _: PaidSubscriptionPlan[_, _]) => lt
        case (friendX: FreeSubscriptionPlan[_, _], friendY: FreeSubscriptionPlan[_, _]) => {
          if (friendX.start < friendY.start) lt
          else if (friendX.start > friendY.start) gt
          else eq
        }
        case (planX: PaidSubscriptionPlan[_, _], planY: PaidSubscriptionPlan[_, _]) => {
          val priceX = planX.charges.price.prices.head.amount
          val priceY = planY.charges.price.prices.head.amount
          (priceX * 100).toInt - (priceY * 100).toInt
        }
      }
    }
  }

  def bestCancelledPlan[P <: SubscriptionPlan.AnyPlan](sub: Subscription[P]): Option[P] =
    if (sub.isCancelled && sub.termEndDate.isBefore(LocalDate.now()))
      sub.plans.list.sorted(planGoodnessOrder).reverse.headOption
    else None

  case class DiscardedPlan[+P <: SubscriptionPlan.AnyPlan](plan: P, why: String)

  def apply[P <: SubscriptionPlan.AnyPlan](sub: Subscription[P], date: LocalDate): String \/ NonEmptyList[P] = {

    val currentPlans = sub.plans.list.toList.sorted(planGoodnessOrder).reverse.map { plan =>

      //If the sub hasn't been paid yet but has started we should fast-forward to the date of first payment (free trial)
      val dateToCheck = if(sub.startDate <= date && sub.acceptanceDate > date) sub.acceptanceDate else date

      val unvalidated = Validation.s[NonEmptyList[DiscardedPlan[P]]](plan)
      /*
      Note that a Contributor may have future sub.acceptanceDate and plan.startDate values if the user has
      updated their payment amount via MMA since starting the contribution. In this case the alreadyStarted assessment
      just checks that the sub.startDate is before, or the same as, the date received by this function.
      */
      val ensureStarted = unvalidated.ensure(DiscardedPlan(plan, s"hasn't started as of $dateToCheck").wrapNel)(_)
      val alreadyStarted = plan match {
        case _: Contributor => ensureStarted(_ => sub.startDate <= date)
        case _ => ensureStarted(_.start <= dateToCheck)
      }
      val freePlanCancelled = alreadyStarted.ensure(DiscardedPlan(plan, "has a free plan which has been cancelled").wrapNel)(_)
      val contributorPlanCancelled = alreadyStarted.ensure(DiscardedPlan(plan, "has a contributor plan which has been cancelled").wrapNel)(_)
      val paidPlanEnded = alreadyStarted.ensure(DiscardedPlan(plan, "has a paid plan which has ended").wrapNel)(_)
      val digipackGiftEnded = alreadyStarted.ensure(DiscardedPlan(plan, "has a digipack gift plan which has ended").wrapNel)(_)
      plan match {
        case _: FreeSubscriptionPlan[_, _] => freePlanCancelled(_ => !sub.isCancelled)
        case plan: PaidSubscriptionPlan[_, _] if plan.product == Product.Contribution => contributorPlanCancelled(_ => !sub.isCancelled)
        case plan: PaidSubscriptionPlan[_, _] if plan.product == Product.Digipack && plan.charges.billingPeriod == OneTimeChargeBillingPeriod =>
          digipackGiftEnded(_ => sub.termEndDate >= dateToCheck)
        case plan: PaidSubscriptionPlan[_, _] => paidPlanEnded(_ => plan.end >= dateToCheck)
      }
    }

    Sequence(currentPlans.map(_.leftMap(_.map(discard => s"Discarded ${discard.plan.id.get} because it ${discard.why}").list.toList.mkString("\n")).disjunction))
  }
}

object Subscription {
  def partial[P <: SubscriptionPlan.AnyPlan](hasPendingFreePlan: Boolean)(
    id: memsub.Subscription.Id,
    name: memsub.Subscription.Name,
    accountId: memsub.Subscription.AccountId,
    startDate: LocalDate,
    acceptanceDate: LocalDate,
    termStartDate: LocalDate,
    termEndDate: LocalDate,
    casActivationDate: Option[DateTime],
    promoCode: Option[PromoCode],
    isCancelled: Boolean,
    readerType: ReaderType,
    gifteeIdentityId: Option[String],
    autoRenew: Boolean
  )(plans: NonEmptyList[P]): Subscription[P] =
    new Subscription(
      id = id,
      name = name,
      accountId = accountId,
      startDate = startDate,
      acceptanceDate = acceptanceDate,
      termStartDate = termStartDate,
      termEndDate = termEndDate,
      casActivationDate = casActivationDate,
      promoCode = promoCode,
      isCancelled = isCancelled,
      hasPendingFreePlan = hasPendingFreePlan,
      plans = CovariantNonEmptyList(plans.head, plans.tail.toList),
      readerType = readerType,
      gifteeIdentityId = gifteeIdentityId,
      autoRenew = autoRenew
    )
}

object ReaderType {

  case object Direct extends ReaderType {
    val value = "Direct"
  }
  case object Gift extends ReaderType {
    val value = "Gift"
  }
  case object Agent extends ReaderType {
    val value = "Agent"
  }
  case object Student extends ReaderType {
    val value = "Student"
  }
  case object Complementary extends ReaderType {
    val value = "Complementary" //Spelled this way to match value in Saleforce/Zuora
    val alternateSpelling = "Complimentary"
  }
  case object Corporate extends ReaderType {
    val value = "Corporate"
  }
  case object Patron extends ReaderType {
    val value = "Patron"
  }

  def apply(maybeString: Option[String]): ReaderType =
    maybeString.map {
      case Direct.value => Direct
      case Gift.value => Gift
      case Agent.value => Agent
      case Student.value => Student
      case Complementary.value => Complementary
      case Complementary.alternateSpelling => Complementary
      case Corporate.value => Corporate
      case Patron.value => Patron
      case unknown => throw new RuntimeException(s"Unknown reader type: $unknown")
    }.getOrElse(Direct)

}
sealed trait ReaderType {
  def value: String
}
