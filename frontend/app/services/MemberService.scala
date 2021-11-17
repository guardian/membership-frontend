package services

import _root_.services.paymentmethods._
import com.gu.config.DiscountRatePlanIds
import com.gu.i18n.Country
import com.gu.memsub.Subscription.{Feature, RatePlanId}
import com.gu.memsub.services.api.PaymentService
import com.gu.memsub.subsv2.services._
import com.gu.memsub.subsv2._
import com.gu.memsub.util.Timing
import com.gu.monitoring.SafeLogger
import com.gu.salesforce.Tier.{Partner, Patron}
import com.gu.salesforce._
import com.gu.stripe.StripeService
import com.gu.subscriptions.Discounter
import com.gu.zuora.api._
import com.gu.zuora.rest.ZuoraRestService
import com.gu.zuora.soap.models.Results.CreateResult
import com.gu.zuora.soap.models.{Queries => SoapQueries}
import model.Eventbrite.{EBCode, EBOrder, EBTicketClass}
import model.RichEvent.RichEvent
import model.{Benefit => _, _}
import org.joda.time.DateTimeZone
import views.support.MembershipCompat._

import scala.concurrent.{ExecutionContext, Future}

object MemberService {

  def featureIdsForTier(features: Seq[SoapQueries.Feature])(tier: Tier, choice: Set[FeatureChoice]): Seq[Feature.Id] = {
    def chooseFeature(choices: Set[FeatureChoice]): Seq[Feature.Id] =
      features.filter(f => choices.map(_.zuoraCode).contains(f.code))
        .map(_.id)

    tier match {
      case Patron() => chooseFeature(FeatureChoice.all)
      case Partner() => chooseFeature(choice).take(1)
      case _ => Nil
    }
  }

  def getDiscountRatePlanIdsToRemove(current: Seq[SubIds], discounts: DiscountRatePlanIds): Seq[RatePlanId] = current.collect {
    case discount if discount.productRatePlanId == discounts.percentageDiscount.planId => discount.ratePlanId
  }
}

class MemberService(implicit val ec: ExecutionContext) extends api.MemberService {

  import EventbriteService._

  // due to an long standing known bug (you can check out on eventbrite directly), people actually get unlimited complimentary tickets
  // as it never decremented the ticket count, so I've removed the logic completely now
  private def retrieveComplimentaryTickets(sub: Subscription[SubscriptionPlan.Member], event: RichEvent): Seq[EBTicketClass] = {
    event.underlying.internalTicketing.map(_.complimentaryTickets).getOrElse(Nil)
  }

  override def createEBCode(subscriber: com.gu.memsub.Subscriber.Member, event: RichEvent)(implicit services: EventbriteCollectiveServices): Future[Option[EBCode]] = {
    val complimentaryTickets = retrieveComplimentaryTickets(subscriber.subscription, event)
    val code = DiscountCode.generate(s"A_${subscriber.contact.identityId}_${event.underlying.ebEvent.id}")
    val unlockedTickets = complimentaryTickets ++ event.retrieveDiscountedTickets(subscriber.subscription.plan.tier)
    event.service.createOrGetAccessCode(event, code, unlockedTickets)
  }

}
