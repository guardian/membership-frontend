package com.gu.memsub.subsv2.reads

import com.gu.memsub.Subscription.{ProductId, ProductRatePlanChargeId, ProductRatePlanId, SubscriptionRatePlanChargeId}
import com.gu.memsub._
import com.gu.memsub.subsv2.reads.CommonReads._
import com.gu.memsub.subsv2._
import play.api.libs.functional.syntax._
import play.api.libs.json.{Reads, _}

import scalaz.std.list._
import scalaz.syntax.traverse._

object CatJsonReads {

  implicit val catalogZuoraPlanChargeReads: Reads[ZuoraCharge] = (
    (__ \ "id").read[String].map(ProductRatePlanChargeId) and
      (__ \ "pricingSummary").read[PricingSummary] and
      (__ \ "billingPeriod").readNullable[ZBillingPeriod] and
      (__ \ "specificBillingPeriod").readNullable[Int] and
      (__ \ "model").read[String] and
      (__ \ "name").read[String] and
      (__ \ "type").read[String] and
      (__ \ "endDateCondition").read[EndDateCondition] and
      (__ \ "upToPeriods").readNullable[Int] and
      (__ \ "upToPeriodsType").readNullable[UpToPeriodsType]
    ) (ZuoraCharge.apply(_, _, _, _, _, _, _, _, _, _))

  implicit val ProductReads = new Reads[Benefit] {
    override def reads(json: JsValue): JsResult[Benefit] = json match {
      case JsString(id) => Benefit.fromId(id).fold[JsResult[Benefit]](JsError(s"Bad product $id"))(e => JsSuccess(e))
      case a => JsError(s"Malformed product JSON, needed a string but got $a")
    }
  }

  implicit val catalogZuoraPlanBenefitReads: Reads[(ProductRatePlanChargeId, Benefit)] = (
    (__ \ "id").read[String].map(ProductRatePlanChargeId) and
      (__ \ "ProductType__c").read[Benefit]
    ) (_ -> _)

  implicit val listOfProductsReads = new Reads[Map[ProductRatePlanChargeId, Benefit]] {
    override def reads(json: JsValue): JsResult[Map[ProductRatePlanChargeId, Benefit]] = json match {
      case JsArray(vals) => vals
        .map(_.validate[(ProductRatePlanChargeId, Benefit)])
        .filter(_.isSuccess).toList // bad things are happening here, we're chucking away errors
        .sequence[JsResult, (ProductRatePlanChargeId, Benefit)]
        .map(_.toMap)
      case _ => JsError("No valid benefits found")
    }
  }

  implicit val statusReads: Reads[Status] = new Reads[Status] {
    override def reads(json: JsValue): JsResult[Status] = json match {
      case JsString("Expired") => JsSuccess(Status.legacy)
      case JsString("Active") => JsSuccess(Status.current)
      case JsString("NotStarted") => JsSuccess(Status.upcoming)
      case a => JsError(s"Unknown status $a")
    }
  }

  def catalogZuoraPlanReads(productType: Option[String], pid: ProductId): Reads[CatalogZuoraPlan] =
    (json: JsValue) => {
      ((__ \ "id").read[String].map(ProductRatePlanId) and
        (__ \ "name").read[String] and
        (__ \ "description").readNullable[String].map(_.mkString) and
        Reads.pure(pid) and
        (__ \ "Saving__c").readNullable[String] and
        (__ \ "productRatePlanCharges").read[List[ZuoraCharge]](niceListReads(catalogZuoraPlanChargeReads)) and
        (__ \ "productRatePlanCharges").read[Map[ProductRatePlanChargeId, Benefit]](listOfProductsReads) and
        (__ \ "status").read[Status] and
        (__ \ "FrontendId__c").readNullable[String].map(_.flatMap(FrontendId.get)) and
        Reads.pure(productType)
        ) (CatalogZuoraPlan.apply _).reads(json)
    }

  implicit val catalogZuoraPlanListReads: Reads[List[CatalogZuoraPlan]] =
    (json: JsValue) =>
      json \ "products" match {
        case JsDefined(JsArray(products)) =>
          products.toList.map { product =>
            val productId = (product \ "id").as[String]
            val productType = (product \ "ProductType__c").asOpt[String]
            val reads = catalogZuoraPlanReads(productType, ProductId(productId))
            (product \ "productRatePlans").validate[List[CatalogZuoraPlan]](niceListReads(reads))
          }
            .filter(_.isSuccess).sequence[JsResult, List[CatalogZuoraPlan]].map(_.flatten)
        case a => JsError(s"No product array found, got $a")
      }
}
