package services

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import com.gu.identity.model.User

import com.gu.membership.salesforce._
import com.gu.membership.salesforce.Member.Keys
import com.gu.membership.util.Timing

import configuration.Config
import controllers.IdentityRequest
import forms.MemberForm._
import model.Eventbrite.{RichEvent, EBCode}
import model.Stripe.{Customer, Card}
import model.{PaidTierPlan, FriendTierPlan, TierPlan}
import monitoring.MemberMetrics
import utils.ScheduledTask

case class MemberServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}

trait MemberService {
  def initialData(user: User, formData: JoinForm) = Map(
    Keys.EMAIL -> user.getPrimaryEmailAddress,
    Keys.FIRST_NAME -> formData.name.first,
    Keys.LAST_NAME -> formData.name.last,
    Keys.MAILING_STREET -> formData.deliveryAddress.line,
    Keys.MAILING_CITY -> formData.deliveryAddress.town,
    Keys.MAILING_STATE -> formData.deliveryAddress.countyOrState,
    Keys.MAILING_POSTCODE -> formData.deliveryAddress.postCode,
    Keys.MAILING_COUNTRY -> formData.deliveryAddress.country.alpha2,
    Keys.ALLOW_MEMBERSHIP_MAIL -> true
  ) ++
    formData.marketingChoices.thirdParty.map( Keys.ALLOW_THIRD_PARTY_EMAIL -> _) ++
    formData.marketingChoices.gnm.map( Keys.ALLOW_GU_RELATED_MAIL -> _)

  def memberData(tier: Tier.Tier, customerOpt: Option[Customer]) = Map(
    Keys.TIER -> tier.toString
  ) ++ customerOpt.map { customer =>
    Map(
      Keys.CUSTOMER_ID -> customer.id,
      Keys.DEFAULT_CARD_ID -> customer.card.id
    )
  }.getOrElse(Map.empty)

  def createMember(user: User, formData: JoinForm, identityRequest: IdentityRequest): Future[String] =
    Timing.record(MemberMetrics, "createMember") {
      def futureCustomerOpt = formData match {
        case paid: PaidMemberJoinForm => StripeService.Customer.create(user.id, paid.payment.token).map(Some(_))
        case friend: FriendJoinForm => Future.successful(None)
      }

      formData.password.map(IdentityService.updateUserPassword(_, identityRequest, user.id))

      for {
        customerOpt <- futureCustomerOpt
        memberId <- MemberRepository.upsert(user.id, initialData(user, formData))
        subscription <- SubscriptionService.createSubscription(memberId, formData, customerOpt)

        // Set some fields once subscription has been successful
        updatedMember <- MemberRepository.upsert(user.id, memberData(formData.tierPlan.tier, customerOpt))
      } yield {
        IdentityService.updateUserFieldsBasedOnJoining(user, formData, identityRequest)

        MemberMetrics.putSignUp(formData.tierPlan.tier)
        memberId.account
      }
    }

  def createDiscountForMember(member: Member, event: RichEvent): Future[Option[EBCode]] = {
    member.tier match {
      case Tier.Friend => Future.successful(None)

      case _ =>
        if (event.memberTickets.nonEmpty) {
          // Add a "salt" to make access codes different to discount codes
          val code = DiscountCode.generate(s"A_${member.identityId}_${event.id}")
          GuardianLiveEventService.createOrGetAccessCode(event, code, event.memberTickets).map(Some(_))
        } else {
          val code = DiscountCode.generate(s"${member.identityId}_${event.id}")
          GuardianLiveEventService.createOrGetDiscount(event.id, code).map(Some(_))
        }
    }
  }

  def updateDefaultCard(member: PaidMember, token: String): Future[Card] = {
    for {
      customer <- StripeService.Customer.updateCard(member.stripeCustomerId, token)
      memberId <- MemberRepository.upsert(member.identityId, Map(Keys.DEFAULT_CARD_ID -> customer.card.id))
    } yield customer.card
  }

  def cancelSubscription(member: Member): Future[String] = {
    for {
      subscription <- SubscriptionService.cancelSubscription(member.salesforceAccountId, member.tier == Tier.Friend)
    } yield {
      MemberMetrics.putCancel(member.tier)
      ""
    }
  }

  def downgradeSubscription(member: Member, tierPlan: TierPlan): Future[String] = {
    for {
      _ <- SubscriptionService.downgradeSubscription(member.salesforceAccountId, FriendTierPlan)
    } yield {
      MemberMetrics.putDowngrade(tierPlan.tier)
      ""
    }
  }

  // TODO: this currently only handles free -> paid
  def upgradeSubscription(member: FreeMember, user: User, newTier: Tier.Tier, form: PaidMemberChangeForm, identityRequest: IdentityRequest): Future[String] = {
    for {
      customer <- StripeService.Customer.create(user.id, form.payment.token)
      paymentResult <- SubscriptionService.createPaymentMethod(member.salesforceAccountId, customer)
      subscriptionResult <- SubscriptionService.upgradeSubscription(member.salesforceAccountId, PaidTierPlan(newTier, form.payment.annual))
      memberId <- MemberRepository.upsert(member.identityId, memberData(newTier, Some(customer)))
    } yield {
      IdentityService.updateUserFieldsBasedOnUpgrade(user, form, identityRequest)
      MemberMetrics.putUpgrade(newTier)
      memberId.account
    }
  }
}

object MemberService extends MemberService

object MemberRepository extends MemberRepository with ScheduledTask[Authentication] {
  val initialValue = Authentication("", "")
  val initialDelay = 0.seconds
  val interval = 30.minutes

  def refresh() = salesforce.getAuthentication

  val salesforce = new Scalaforce {
    val config = Config.touchpointBackendConfig.salesforce

    val consumerKey = config.consumerKey
    val consumerSecret = config.consumerSecret

    val apiURL = config.apiURL
    val apiUsername = config.apiUsername
    val apiPassword = config.apiPassword
    val apiToken = config.apiToken

    val stage = Config.stage
    val application = "Frontend"

    def authentication: Authentication = agent.get()
  }
}
