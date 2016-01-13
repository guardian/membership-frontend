package services.api

import com.gu.identity.play.IdMinimalUser
import com.gu.memsub.Subscription.ProductRatePlanId
import com.gu.memsub.{Paid, Subscription}
import com.gu.salesforce.{Tier, ContactId, PaidTier}
import com.gu.stripe.Stripe
import com.gu.zuora.soap.models.Queries.PreviewInvoiceItem
import com.gu.zuora.soap.models.Results.{CreateResult, SubscribeResult}
import controllers.IdentityRequest
import forms.MemberForm.{FreeMemberChangeForm, JoinForm, PaidMemberChangeForm, PaidMemberJoinForm}
import model.Eventbrite.{EBCode, EBOrder, EBTicketClass}
import model.RichEvent.RichEvent
import model.{FreeSFMember, GenericSFContact, PaidSFMember, SFMember}
import views.support.ThankyouSummary

import scala.concurrent.Future
import scalaz.\/

trait MemberService {
  import MemberService._

  def createMember(user: IdMinimalUser,
                   formData: JoinForm,
                   identityRequest: IdentityRequest,
                   fromEventId: Option[String]): Future[ContactId]

  def previewUpgradeSubscription(subscription: Subscription with Paid,
                                 newPlanId: ProductRatePlanId): Future[Seq[PreviewInvoiceItem]]

  def upgradeFreeSubscription(freeMember: FreeSFMember,
                              newTier: PaidTier,
                              form: FreeMemberChangeForm,
                              identityRequest: IdentityRequest): Future[MemberError \/ ContactId]

  def upgradePaidSubscription(paidMember: PaidSFMember,
                              newTier: PaidTier,
                              form: PaidMemberChangeForm,
                              identityRequest: IdentityRequest): Future[MemberError \/ContactId]

  def downgradeSubscription(contact: SFMember, user: IdMinimalUser): Future[MemberError \/ String]

  def cancelSubscription(contact: SFMember, user: IdMinimalUser): Future[MemberError \/ String]

  def subscriptionUpgradableTo(memberId: SFMember, tier: PaidTier): Future[Option[Subscription]]

  def getMembershipSubscriptionSummary(contact: GenericSFContact): Future[ThankyouSummary]

  /*
   * If the member is entitled to complimentary tickets return its Zuora account's corresponding usage records count.
   * Returns none otherwise
   */
  def getUsageCountWithinTerm(subscription: Subscription, unitOfMeasure: String): Future[Option[Int]]

  def recordFreeEventUsage(member: SFMember,
                           event: RichEvent,
                           order: EBOrder,
                           quantity: Int): Future[CreateResult]

  def retrieveComplimentaryTickets(member: SFMember, event: RichEvent): Future[Seq[EBTicketClass]]

  def createEBCode(member: SFMember, event: RichEvent): Future[Option[EBCode]]

  def createPaidSubscription(contactId: ContactId,
                             joinData: PaidMemberJoinForm,
                             customer: Stripe.Customer): Future[SubscribeResult]

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
