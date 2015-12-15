package services.api

import com.gu.identity.play.IdMinimalUser
import com.gu.salesforce.{ContactId, PaidTier}
import com.gu.stripe.Stripe
import com.gu.zuora.api.ZuoraService.FeatureId
import com.gu.zuora.soap.models.Queries.PreviewInvoiceItem
import com.gu.zuora.soap.models.Results.{SubscribeResult, CreateResult}
import controllers.IdentityRequest
import forms.MemberForm.{PaidMemberJoinForm, FreeMemberChangeForm, JoinForm, PaidMemberChangeForm}
import model.Eventbrite.{EBCode, EBTicketClass, EBOrder}
import model.RichEvent.RichEvent
import model._
import views.support.ThankyouSummary

import scala.concurrent.Future

trait MemberService {
  def currentSubscription(contact: ContactId): Future[model.Subscription]

  def currentPaidSubscription(contact: ContactId): Future[model.PaidSubscription]

  def createMember(user: IdMinimalUser,
                   formData: JoinForm,
                   identityRequest: IdentityRequest,
                   fromEventId: Option[String]): Future[ContactId]

  def previewUpgradeSubscription(subscription: model.PaidSubscription,
                                 newTier: PaidTier): Future[Seq[PreviewInvoiceItem]]

  def upgradeFreeSubscription(freeMember: FreeSFMember,
                              newTier: PaidTier,
                              form: FreeMemberChangeForm,
                              identityRequest: IdentityRequest): Future[ContactId]

  def upgradePaidSubscription(paidMember: PaidSFMember,
                              newTier: PaidTier,
                              form: PaidMemberChangeForm,
                              identityRequest: IdentityRequest): Future[ContactId]

  // TODO: why do we return a String?
  def downgradeSubscription(contact: SFMember, user: IdMinimalUser): Future[String]

  // TODO: why do we return a String?
  def cancelSubscription(contact: SFMember, user: IdMinimalUser): Future[String]

  def subscriptionUpgradableTo(memberId: SFMember, targetTier: PaidTier): Future[Option[model.Subscription]]

  def updateDefaultCard(member: PaidSFMember, token: String): Future[Stripe.Card]

  def getMembershipSubscriptionSummary(contact: GenericSFContact): Future[ThankyouSummary]

  /*
   * If the member is entitled to complimentary tickets return its Zuora account's corresponding usage records count.
   * Returns none otherwise
   */
  def getUsageCountWithinTerm(subscription: model.Subscription, unitOfMeasure: String): Future[Option[Int]]

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

  def chooseFeature(choices: Set[FeatureChoice]): Future[Seq[FeatureId]]
}
