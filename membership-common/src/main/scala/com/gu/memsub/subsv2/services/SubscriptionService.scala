package com.gu.memsub.subsv2.services

import com.github.nscala_time.time.Imports._
import com.gu.memsub
import com.gu.memsub.Subscription.{AccountId, ProductRatePlanId, RatePlanId}
import com.gu.memsub.subsv2.SubscriptionPlan.{AnyPlan, _}
import com.gu.memsub.subsv2._
import com.gu.memsub.subsv2.reads.ChargeListReads.ProductIds
import com.gu.memsub.subsv2.reads.CommonReads._
import com.gu.memsub.subsv2.reads.SubJsonReads._
import com.gu.memsub.subsv2.reads.SubPlanReads
import com.gu.memsub.subsv2.services.SubscriptionService.{CatalogMap, SoapClient}
import com.gu.memsub.subsv2.services.SubscriptionTransform.getRecentlyCancelledSubscriptions
import com.gu.salesforce.ContactId
import com.gu.zuora.rest.SimpleClient
import com.gu.monitoring.SafeLogger
import org.joda.time.{LocalDate, LocalTime}
import play.api.libs.json.{Reads => JsReads, _}
import scala.language.higherKinds
import scala.util.Try
import scalaz._
import scalaz.syntax.all._
import scalaz.syntax.std.either._
import scalaz.syntax.std.option._

object SubscriptionService {
  type SoapClient[M[_]] = ContactId => M[List[memsub.Subscription.AccountId]]
  type CatalogMap = Map[ProductRatePlanId, CatalogZuoraPlan]
}

/*
Sequence turns a list of either into an either of list.  In this case, it does it by putting all the rights into a list and returning
that as a right.  However if there are no rights, it will return a left of any lefts.
This is mostly useful if we want to try a load of things and hopefully one will succeed.  It's not too good in case things
go wrong, we don't know which ones should have failed and which shouldn't have.  But at least it keeps most of the errors.
 */
object Sequence {

  def apply[A](eitherList: List[String \/ A]): String \/ NonEmptyList[A] = {
    val zero = (List[String](), List[A]())
    val product = eitherList.foldRight(zero)({
      case (-\/(left), (accuLeft, accuRight)) => (left :: accuLeft, accuRight)
      case (\/-(right), (accuLeft, accuRight)) => (accuLeft, right :: accuRight)
    })
    // if any are right, return them all, otherwise return all the left
    product match {
      case (Nil, Nil) => -\/("no subscriptions found at all, even invalid ones") // no failures or successes
      case (errors, Nil) => -\/(errors.mkString("\n")) // no successes
      case (_, result :: results) => \/-(NonEmptyList.fromSeq(result, results)) // discard some errors as long as some worked (log it?)
    }
  }

}

// since we don't have a stack to trace, we need to make our own
object Trace {

  implicit class Traceable[T](t: String \/ T) {
    def withTrace(message: String): String \/ T = t match {
      case -\/(e) => -\/(s"$message: {$e}")
      case right => right
    }
  }

}

import com.gu.memsub.subsv2.services.Trace.Traceable

class SubscriptionService[M[_]](pids: ProductIds, futureCatalog: => M[CatalogMap], rest: SimpleClient[M], soap: SoapClient[M])(implicit t: Monad[M]) {
  type EitherTM[A] = EitherT[String, M, A]

  private implicit val idReads = new JsReads[JsValue] {
    override def reads(json: JsValue): JsResult[JsValue] = JsSuccess(json)
  }

  def jsonSubscriptionsFromContact(contact: ContactId): M[Disjunction[String, List[JsValue]]] = {
    (for {
      account <- ListT[EitherTM, AccountId](EitherT[String, M, IList[AccountId]](soap.apply(contact).map(l => \/.r[String](IList.fromSeq(l)))))
      subJson <- ListT[EitherTM, JsValue](EitherT(jsonSubscriptionsFromAccount(account)).map(IList.fromSeq))
    } yield subJson).toList.run
  }

  def jsonSubscriptionsFromAccount(accountId: AccountId): M[Disjunction[String, List[JsValue]]] =
    rest.get[List[JsValue]](s"subscriptions/accounts/${accountId.get}")(multiSubJsonReads)


  /**
    * find the best current subscription for the salesforce contact
    * TODO get rid of this and use pattern matching instead
    */
  def either[FALLBACK <: AnyPlan, PREFERRED <: AnyPlan](contact: ContactId)(implicit a: SubPlanReads[FALLBACK], b: SubPlanReads[PREFERRED]): M[\/[String, Option[Subscription[FALLBACK] \/ Subscription[PREFERRED]]]] = {
    val futureSubJson = jsonSubscriptionsFromContact(contact)
    futureSubJson.flatMap { subJsonsEither =>
      futureCatalog.map { catalog =>
        subJsonsEither.leftMap(e => s"Error from sub service for sf contact $contact: $e").map { subJson =>
          SubscriptionTransform.tryTwoReadersForSubscriptionJson[PREFERRED, FALLBACK](catalog, pids)(subJson)
            .leftMap(e => SafeLogger.debug(s"Error from tryTwoReadersForSubscriptionJson for sf contact $contact: $e"))
            .fold(_ => None, Some.apply)
        }
      }
    }
  }
}

// this is (all?) the testable stuff without mocking needed
// we should make the subscription service just getting the json, and then we can have testable pure functions here
object SubscriptionTransform {

  val subIdsReads: JsReads[SubIds] = new JsReads[SubIds] {
    override def reads(json: JsValue): JsResult[SubIds] = {

      (
        (json \ "id").validate[String].map(RatePlanId) |@|
          (json \ "productRatePlanId").validate[String].map(ProductRatePlanId)
        ) (SubIds)
    }
  }

  def backdoorRatePlanIdsFromJson(subJson: JsValue): Disjunction[String, List[SubIds]] = {
    val ids = (subJson \ "ratePlans").validate[List[SubIds]](niceListReads(subIdsReads)).asEither.disjunction.leftMap(_.toString)
    // didn't actually check if they're current

    ids.leftMap { error =>
      SafeLogger.warn(s"Error from sub service for json: $error")
    }

    ids
  }

  def tryTwoReadersForSubscriptionJson[PREFERRED <: AnyPlan : SubPlanReads, FALLBACK <: AnyPlan : SubPlanReads](catalog: CatalogMap, pids: ProductIds)(subJsons: List[JsValue]): \/[String,Disjunction[Subscription[FALLBACK], Subscription[PREFERRED]]] = {
    val maybePreferred = getCurrentSubscriptions[PREFERRED](catalog, pids)(subJsons).map(_.head /*if more than one current, just pick one (for now!)*/)
    lazy val maybeFallback = getCurrentSubscriptions[FALLBACK](catalog, pids)(subJsons).map(_.head /*if more than one current, just pick one (for now!)*/)
    maybePreferred match {
      case \/-(preferredSub) => \/.right(\/-(preferredSub))
      case -\/(err1) => maybeFallback match {
        case \/-(fallbackSub) => \/.right(-\/(fallbackSub))
        case -\/(err2) => \/.left(s"Error from sub service: $err1\n\n$err2")
      }
    }
  }

  type TimeRelativeSubTransformer[P <: AnyPlan] = (CatalogMap, ProductIds) => List[JsValue] => Disjunction[String, NonEmptyList[Subscription[P]]]

  def getCurrentSubscriptions[P <: AnyPlan : SubPlanReads](catalog: CatalogMap, pids: ProductIds)(subJsons: List[JsValue]): Disjunction[String, NonEmptyList[Subscription[P]]] = {

    def getFirstCurrentSub[P <: AnyPlan](subs: NonEmptyList[Subscription[P]]): String \/ NonEmptyList[Subscription[P]] = // just quickly check to find one with a current plan
      Sequence(subs.map { sub =>
        Try {
          sub.plan // just to force a throw if it doesn't have one
        } match {
          case scala.util.Success(_) => \/-(sub): \/[String, Subscription[P]]
          case scala.util.Failure(ex) => -\/(ex.toString): \/[String, Subscription[P]]
        }
      }.list.toList)

    Sequence(subJsons.map { subJson =>
      getSubscription(catalog, pids)(subJson)
    }).flatMap(getFirstCurrentSub[P])
  }

  def getRecentlyCancelledSubscriptions[P <: AnyPlan : SubPlanReads](
    today: LocalDate,
    lastNMonths: Int, // cancelled in the last n months
    catalog: CatalogMap,
    pids: ProductIds,
    subJsons: List[JsValue]
  ): Disjunction[String, List[Subscription[P]]] = {
    import Scalaz._
    subJsons
      .map(getSubscription[P](catalog, pids))
      .sequence
      .map { _.filter { sub =>
        sub.isCancelled &&
          (sub.termEndDate isAfter today.minusMonths(lastNMonths)) &&
          (sub.termEndDate isBefore today)
      }
      }
  }

  def getSubscription[P <: AnyPlan : SubPlanReads](catalog: CatalogMap, pids: ProductIds, now: () => LocalDate = LocalDate.now/*now only needed for pending friend downgrade*/)(subJson: JsValue): Disjunction[String, Subscription[P]] = {
    val planToSubscriptionFunction = subscriptionReads[P](now()).reads(subJson).asEither.disjunction.leftMap(_.mkString(" ")).withTrace("planToSubscriptionFunction")

    val lowLevelPlans = subJson.validate[List[SubscriptionZuoraPlan]](subZuoraPlanListReads).asEither.disjunction.leftMap(_.toString).withTrace("validate-lowLevelPlans")
    lowLevelPlans.flatMap { lowLevelPlans =>

      val validHighLevelPlans: String \/ NonEmptyList[P] = Sequence(lowLevelPlans.map { lowLevelPlan =>
        // get the equivalent plan from the catalog so we can merge them into a standard high level object
        catalog.get(lowLevelPlan.productRatePlanId).toRightDisjunction(s"No catalog plan - prpId = ${lowLevelPlan.productRatePlanId}").flatMap { catalogPlan =>
          val maybePlans = implicitly[SubPlanReads[P]].read(pids, lowLevelPlan, catalogPlan)
          maybePlans.toDisjunction.leftMap(_.list.zipWithIndex.map{
            case (err, index) => s"  ${index+1}: $err"
          }.toList.mkString("\n", "\n", "\n")).withTrace(s"high-level-plan-read: ${lowLevelPlan.id}")
        }
      })

      // now wrap them in a subscription
      validHighLevelPlans.flatMap(highLevelPlans =>
        planToSubscriptionFunction.map(_.apply(highLevelPlans))
      )
    }
  }

}

case class SubIds(ratePlanId: RatePlanId, productRatePlanId: ProductRatePlanId)
