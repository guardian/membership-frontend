package services.api

import com.gu.membership.model.TierPlan
import com.gu.membership.salesforce.{PaidTier, ContactId}
import com.gu.membership.stripe.Stripe
import com.gu.membership.zuora.soap.models.Queries.PreviewInvoiceItem
import com.gu.membership.zuora.soap.models.Results.{AmendResult, SubscribeResult, UpdateResult}
import com.gu.membership.zuora.soap.models.{Queries => SoapQueries}
import forms.MemberForm.{JoinForm, PaidMemberJoinForm}
import model.{SFMember, FeatureChoice}

import scala.concurrent.Future

trait ZuoraService {
  def getSubscriptionsByCasId(casId: String): Future[Seq[SoapQueries.Subscription]]

  def currentSubscription(contact: ContactId): Future[model.Subscription]

  def currentPaidSubscription(contact: ContactId): Future[model.PaidSubscription]

  def createPaidSubscription(contactId: ContactId,
                             joinData: PaidMemberJoinForm,
                             customer: Stripe.Customer): Future[SubscribeResult]

  def createFreeSubscription(contactId: ContactId,
                             joinData: JoinForm): Future[SubscribeResult]

  def createPaymentMethod(contactId: ContactId,
                          customer: Stripe.Customer): Future[UpdateResult]

  def upgradeSubscription(contactId: ContactId,
                          newTierPlan: TierPlan,
                          preview: Boolean,
                          featureChoice: Set[FeatureChoice]): Future[AmendResult]

  def previewUpgradeSubscription(subscription: model.Subscription,
                                 contact: SFMember,
                                 newTier: PaidTier): Future[Seq[PreviewInvoiceItem]]
}
