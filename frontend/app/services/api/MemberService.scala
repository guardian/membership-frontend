package services.api

import java.net.InetAddress

import com.gu.i18n.Country
import com.gu.identity.play.IdMinimalUser
import com.gu.memsub.Subscriber._
import com.gu.memsub.promo.{PromoError, Upgrades, ValidPromotion}
import com.gu.memsub.subsv2._
import com.gu.memsub.{BillingSchedule, Subscription => S}
import com.gu.salesforce.{ContactId, PaidTier, Tier}
import com.gu.stripe.Stripe
import com.gu.zuora.soap.models.Results.{CreateResult, SubscribeResult}
import controllers.IdentityRequest
import forms.MemberForm._
import model.Eventbrite.{EBCode, EBOrder, EBTicketClass}
import model.RichEvent.RichEvent
import model.{GenericSFContact, PlanChoice}
import utils.CampaignCode
import views.support.ThankyouSummary

import scala.concurrent.Future
import scalaz.\/

trait MemberService {
  import MemberService._

  type ZuoraSubName = String

  def country(contact: GenericSFContact)(implicit i: IdentityRequest): Future[Country]

  def createMember(user: IdMinimalUser,
                   formData: JoinForm,
                   identityRequest: IdentityRequest,
                   fromEventId: Option[String],
                   campaignCode: Option[CampaignCode],
                   tier: Tier,
                   ipAddress: Option[InetAddress],
                   ipCountry: Option[Country]): Future[(ContactId, ZuoraSubName)]

  def previewUpgradeSubscription(subscriber: PaidMember, newPlan: PlanChoice, code: Option[ValidPromotion[Upgrades]])
                                (implicit i: IdentityRequest): Future[MemberError \/ BillingSchedule]

  def upgradeFreeSubscription(sub: FreeMember, newTier: PaidTier, form: FreeMemberChangeForm, code: Option[CampaignCode])
                             (implicit identity: IdentityRequest): Future[MemberError \/ ContactId]

  def downgradeSubscription(subscriber: PaidMember): Future[MemberError \/ Unit]

  def upgradePaidSubscription(sub: PaidMember, newTier: PaidTier, form: PaidMemberChangeForm, code: Option[CampaignCode])
                             (implicit id: IdentityRequest): Future[MemberError \/ ContactId]

  def cancelSubscription(subscriber: Member): Future[MemberError \/ Unit]

  def subscriptionUpgradableTo(subscription: Subscription[SubscriptionPlan.Member], newTier: PaidTier): Boolean

  def getMembershipSubscriptionSummary(contact: GenericSFContact): Future[ThankyouSummary]

  /*
   * If the member is entitled to complimentary tickets return its Zuora account's corresponding usage records count.
   * Returns none otherwise
   */
  def getUsageCountWithinTerm(subscription: Subscription[SubscriptionPlan.Member], unitOfMeasure: String): Future[Option[Int]]

  def recordFreeEventUsage(subs: Subscription[SubscriptionPlan.Member],
                           event: RichEvent,
                           order: EBOrder,
                           quantity: Int): Future[CreateResult]

  def retrieveComplimentaryTickets(subscription: Subscription[SubscriptionPlan.Member], event: RichEvent): Future[Seq[EBTicketClass]]

  def createEBCode(subscriber: Member, event: RichEvent): Future[Option[EBCode]]

  def createPaidSubscription(contactId: ContactId,
                             joinData: PaidMemberForm,
                             nameData: NameForm,
                             tier: PaidTier,
                             customer: Stripe.Customer,
                             campaignCode: Option[CampaignCode],
                             email: String,
                             ipAddress: Option[InetAddress],
                             ipCountry: Option[Country]): Future[SubscribeResult]

  def createFreeSubscription(contactId: ContactId,
                             joinData: JoinForm,
                             email: String,
                             ipAddress: Option[InetAddress],
                             ipCountry: Option[Country]): Future[SubscribeResult]
}

object MemberService {
  sealed trait MemberError extends Throwable

  case class MemberPromoError(get: PromoError) extends MemberError {
    override def getMessage = s"Promo error: ${get.msg}"
  }

  case class PaidSubscriptionExpected(name: S.Name) extends MemberError {
    override def getMessage = s"Paid subscription expected. Got a free one instead: ${name.get} "
  }
  case class PendingAmendError(name: S.Name) extends MemberError {
    override def getMessage = s"Subscription ${name.get} already has a pending amend"
  }
  case class NoCardError(name: S.Name) extends MemberError {
    override def getMessage = s"Subscription ${name.get} has no card"
  }
}
