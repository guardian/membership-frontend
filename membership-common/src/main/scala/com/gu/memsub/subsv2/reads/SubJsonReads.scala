package com.gu.memsub.subsv2.reads

import com.gu.memsub.Subscription._
import com.gu.memsub.promo.PromoCode
import com.gu.memsub
import com.gu.memsub.{BillingPeriod, PricingSummary}
import com.gu.memsub.subsv2._
import org.joda.time.{DateTime, LocalDate}
import play.api.libs.functional.syntax._
import com.github.nscala_time.time.Imports._
import play.api.libs.json._
import CommonReads._
import com.gu.zuora.rest.{Feature => RestFeature}
import com.gu.zuora.rest.Readers._

import scalaz.syntax.traverse._
import scalaz.std.list._
import scalaz.syntax.applicative._
import scalaz.NonEmptyList

// since we don't have a stack to trace, we need to make our own
object Trace {

  implicit class Traceable[T](result: JsResult[T]) {
    def withTrace(message: String): JsResult[T] = result match {
      case JsError(e) => JsError(s"$message: $e")
      case success => success
    }
  }

}
import Trace.Traceable

object SubJsonReads {

  implicit val zuoraSubscriptionRatePlanChargeReads: Reads[ZuoraCharge] = (
    (__ \ "id").read[String].map(SubscriptionRatePlanChargeId) and
    (__ \ "productRatePlanChargeId").read[String].map(ProductRatePlanChargeId) and
    (__ \ "pricingSummary").read[PricingSummary] and
    (__ \ "billingPeriod").readNullable[ZBillingPeriod] and
    (__ \ "specificBillingPeriod").readNullable[Int] and
    (__ \ "model").read[String] and
    (__ \ "name").read[String] and
    (__ \ "type").read[String] and
    (__ \ "endDateCondition").read[EndDateCondition] and
    (__ \ "upToPeriods").readNullable[Int] and
    (__ \ "upToPeriodsType").readNullable[UpToPeriodsType]
  )(ZuoraCharge.apply(_,_,_,_,_,_,_,_,_,_,_))

  val commonZuoraPlanReads: Reads[SubscriptionZuoraPlan] = new Reads[SubscriptionZuoraPlan] {
    override def reads(json: JsValue): JsResult[SubscriptionZuoraPlan] = {

      // our common zuora plan has effective dates on the plan, but we have them on the charge.
      // FIXME this is evaluated every time it's used which causes any error message to be repeated 3 times
      val dates = (json \ "ratePlanCharges").toOption.collect { case JsArray(chs) => chs.map(c => (
        (c \ "effectiveStartDate").validate[LocalDate].withTrace("effectiveStartDate") |@|
        (c \ "effectiveEndDate").validate[LocalDate].withTrace("effectiveEndDate") |@|
        (c \ "chargedThroughDate").validateOpt[LocalDate].withTrace("chargedThroughDate")).tupled
      )}.toSeq.flatten.toList.sequence[JsResult, (LocalDate, LocalDate, Option[LocalDate])]

      (
        (json \ "id").validate[String].map(RatePlanId) |@|
        (json \ "productRatePlanId").validate[String].map(ProductRatePlanId) |@|
        (json \ "productName").validate[String] |@|
        (json \ "subscriptionProductFeatures").validate[List[RestFeature]] |@|
        dates.map(_.map(_._3).sorted.reduceOption(_ orElse _).flatten) |@|
        (json \ "ratePlanCharges").validate[NonEmptyList[ZuoraCharge]](nelReads(niceListReads(zuoraSubscriptionRatePlanChargeReads))) |@|
        dates.flatMap(_.map(_._1).sorted.headOption.fold[JsResult[LocalDate]](JsError("Missing start"))(JsSuccess(_))) |@|
        dates.flatMap(_.map(_._2).sorted.headOption.fold[JsResult[LocalDate]](JsError("Missing end"))(JsSuccess(_)))
      )(SubscriptionZuoraPlan).withTrace("low-level-plan")
    }
  }

  implicit val subZuoraPlanListReads: Reads[List[SubscriptionZuoraPlan]] = new Reads[List[SubscriptionZuoraPlan]] {
    override def reads(json: JsValue): JsResult[List[SubscriptionZuoraPlan]] = {
      (json \ "ratePlans").validate[List[SubscriptionZuoraPlan]](niceListReads(commonZuoraPlanReads))
    }
  }

  val multiSubJsonReads: Reads[List[JsValue]] = new Reads[List[JsValue]] {
    override def reads(json: JsValue): JsResult[List[JsValue]] = json \ "subscriptions" match {
      case JsDefined(JsArray(subs)) => JsSuccess(subs.toList)
      case _ => JsError("Found no subs")
    }
  }

  implicit val lenientDateTimeReader: Reads[DateTime] =
    JodaReads.DefaultJodaDateTimeReads orElse Reads.IsoDateReads.map(new DateTime(_))

  def subscriptionReads[P <: SubscriptionPlan.AnyPlan](now: LocalDate/*TODO get rid when we fix the below*/): Reads[NonEmptyList[P] => Subscription[P]] = new Reads[NonEmptyList[P] => Subscription[P]] {
    override def reads(json: JsValue): JsResult[NonEmptyList[P] => Subscription[P]] = {

      // ideally we'd use the plans list
      // on the main subscription model, but this is a quick fix.
      val hasPendingFreePlan: Boolean = json \ "ratePlans" match {
        case JsDefined(JsArray(plans)) => plans.exists { plan =>
          val prices = (plan \\ "price").flatMap(_.asOpt[Float])
          val chargeStartDates = (plan \\ "effectiveStartDate").flatMap(_.asOpt[LocalDate])
          val planAddedThroughAmendment = (plan \ "lastChangeType").asOpt[String].contains("Add")
          chargeStartDates.forall(_ >= now) && prices.forall(_ == 0f) && planAddedThroughAmendment
        }
        case _ => false
      }

      json match {
        case o: JsObject => (
          (__ \ "id").read[String].map(memsub.Subscription.Id) and
          (__ \ "subscriptionNumber").read[String].map(memsub.Subscription.Name) and
          (__ \ "accountId").read[String].map(memsub.Subscription.AccountId) and
          (__ \ "contractEffectiveDate").read[LocalDate] and
          (__ \ "customerAcceptanceDate").read[LocalDate] and
          (__ \ "termStartDate").read[LocalDate] and
          (__ \ "termEndDate").read[LocalDate] and
          (__ \ "ActivationDate__c").readNullable[DateTime](lenientDateTimeReader) and
          (__ \ "PromotionCode__c").readNullable[String].map(_.map(PromoCode)) and
          (__ \ "status").read[String].map(_ == "Cancelled") and
          (__ \ "ReaderType__c").readNullable[String].map(ReaderType.apply) and
          (__ \ "GifteeIdentityId__c").readNullable[String] and
          (__ \ "autoRenew").read[Boolean]
        )(memsub.subsv2.Subscription.partial[P](hasPendingFreePlan) _).reads(o)
        case e => JsError(s"Needed a JsObject, got ${e.getClass.getSimpleName}")
      }
    }
  }
}
