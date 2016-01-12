package services.api

import com.gu.identity.play.IdMinimalUser
import com.gu.membership.{FreeMembershipPlan, PaidMembershipPlan, MembershipPlan}
import com.gu.memsub.Subscriber._
import com.gu.memsub.Subscription.{Plan, Paid, ProductRatePlanId}
import com.gu.memsub._
import com.gu.salesforce.{Tier, ContactId, PaidTier}
import com.gu.stripe.Stripe
import com.gu.zuora.soap.models.Queries.PreviewInvoiceItem
import com.gu.zuora.soap.models.Results.{CreateResult, SubscribeResult}
import controllers.IdentityRequest
import forms.MemberForm.{FreeMemberChangeForm, JoinForm, PaidMemberChangeForm, PaidMemberJoinForm}
import model.Eventbrite.{EBCode, EBOrder, EBTicketClass}
import model.RichEvent.RichEvent
import model.{GenericSFContact}
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

  def previewUpgradeSubscription(subscription: Subscription with Paid,
                                 newPlanId: ProductRatePlanId): Future[Seq[PreviewInvoiceItem]]

  def upgradeFreeSubscription(subscriber: FreeMember,
                              newTier: PaidTier,
                              form: FreeMemberChangeForm,
                              identityRequest: IdentityRequest,
                              campaignCode: Option[CampaignCode]): Future[MemberError \/ ContactId]

  def downgradeSubscription(subscriber: PaidMember): Future[MemberError \/ Unit]

  def upgradePaidSubscription(subscriber: PaidMember,
                              newTier: PaidTier,
                              form: PaidMemberChangeForm,
                              identityRequest: IdentityRequest,
                              campaignCode: Option[CampaignCode]): Future[MemberError \/ContactId]

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
}
