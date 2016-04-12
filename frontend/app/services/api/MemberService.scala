package services.api

import com.gu.identity.play.IdMinimalUser
import com.gu.memsub.Subscriber._
import com.gu.memsub.Subscription.{Plan, Paid, ProductRatePlanId}
import com.gu.memsub._
import com.gu.memsub.promo.PromoCode
import com.gu.salesforce.{ContactId, PaidTier}
import com.gu.services.model.BillingSchedule
import com.gu.stripe.Stripe
import com.gu.zuora.soap.models.Results.{CreateResult, SubscribeResult}
import controllers.IdentityRequest
import forms.MemberForm.{FreeMemberChangeForm, JoinForm, PaidMemberChangeForm, PaidMemberJoinForm}
import model.Eventbrite.{EBCode, EBOrder, EBTicketClass}
import model.RichEvent.RichEvent
import model.{PlanChoice, GenericSFContact}
import views.support.ThankyouSummary

import scala.concurrent.Future
import scalaz.\/
import utils.CampaignCode

trait MemberService {
  import MemberService._

  def createMember(user: IdMinimalUser,
                   formData: JoinForm,
                   identityRequest: IdentityRequest,
                   fromEventId: Option[String],
                   campaignCode: Option[CampaignCode]): Future[ContactId]

  def previewUpgradeSubscription(subscriber: PaidMember, newPlan: PlanChoice, code: Option[PromoCode])
                                (implicit i: IdentityRequest): Future[MemberError \/ BillingSchedule]

  def upgradeFreeSubscription(sub: FreeMember, newTier: PaidTier, form: FreeMemberChangeForm, code: Option[CampaignCode])
                             (implicit identity: IdentityRequest): Future[MemberError \/ ContactId]

  def downgradeSubscription(subscriber: PaidMember): Future[MemberError \/ Unit]

  def upgradePaidSubscription(sub: PaidMember, newTier: PaidTier, form: PaidMemberChangeForm, code: Option[CampaignCode])
                             (implicit id: IdentityRequest): Future[MemberError \/ ContactId]

  def cancelSubscription(subscriber: Member): Future[MemberError \/ Unit]

  def subscriptionUpgradableTo(subscription: Subscription with PaymentStatus[Plan], newTier: PaidTier): Boolean

  def getMembershipSubscriptionSummary(contact: GenericSFContact): Future[ThankyouSummary]

  /*
   * If the member is entitled to complimentary tickets return its Zuora account's corresponding usage records count.
   * Returns none otherwise
   */
  def getUsageCountWithinTerm(subscription: Subscription, unitOfMeasure: String): Future[Option[Int]]

  def recordFreeEventUsage(subs: Subscription,
                           event: RichEvent,
                           order: EBOrder,
                           quantity: Int): Future[CreateResult]

  def retrieveComplimentaryTickets(subscription: Subscription, event: RichEvent): Future[Seq[EBTicketClass]]

  def createEBCode(subscriber: Member, event: RichEvent): Future[Option[EBCode]]

  def createPaidSubscription(contactId: ContactId,
                             joinData: PaidMemberJoinForm,
                             customer: Stripe.Customer,
                             campaignCode: Option[CampaignCode]): Future[SubscribeResult]

  def createFreeSubscription(contactId: ContactId,
                             joinData: JoinForm): Future[SubscribeResult]
}

object MemberService {
  sealed trait MemberError extends Throwable

  case class PaidSubscriptionExpected(name: Subscription.Name) extends MemberError {
    override def getMessage = s"Paid subscription expected. Got a free one instead: ${name.get} "
  }
  case class PendingAmendError(name: Subscription.Name) extends MemberError {
    override def getMessage = s"Subscription ${name.get} already has a pending amend"
  }
  case class NoCardError(name: Subscription.Name) extends MemberError {
    override def getMessage = s"Subscription ${name.get} has no card"
  }
}
