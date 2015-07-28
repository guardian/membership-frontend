package services

import com.gu.identity.play.IdMinimalUser
import com.gu.membership.model.{FriendTierPlan, ProductRatePlan}
import com.gu.membership.salesforce.Member.Keys
import com.gu.membership.salesforce._
import com.gu.membership.stripe.{Stripe, StripeService}
import com.gu.membership.touchpoint.TouchpointBackendConfig
import com.gu.monitoring.StatusMetrics
import configuration.Config
import monitoring.TouchpointBackendMetrics
import play.api.libs.json.Json
import services.zuora.ZuoraService
import tracking._
import utils.TestUsers.isTestUser

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object TouchpointBackend {
  import TouchpointBackendConfig.BackendType

  def apply(backendType: TouchpointBackendConfig.BackendType): TouchpointBackend =
    TouchpointBackend(TouchpointBackendConfig.byType(backendType, Config.config))

  def apply(touchpointBackendConfig: TouchpointBackendConfig): TouchpointBackend = {

    val stripeService = new StripeService(touchpointBackendConfig.stripe, new TouchpointBackendMetrics with StatusMetrics {
      val backendEnv = touchpointBackendConfig.stripe.envName
      val service = "Stripe"
    })

    val zuoraService = new ZuoraService(touchpointBackendConfig.zuora)

    val memberRepository = new FrontendMemberRepository(touchpointBackendConfig.salesforce)

    TouchpointBackend(memberRepository, stripeService, zuoraService, touchpointBackendConfig.productRatePlans)
  }

  val Normal = TouchpointBackend(BackendType.Default)
  val TestUser = TouchpointBackend(BackendType.Testing)

  val All = Seq(Normal, TestUser)

  def forUser(user: IdMinimalUser) = if (isTestUser(user)) TestUser else Normal
}

case class TouchpointBackend(
  memberRepository: FrontendMemberRepository,
  stripeService: StripeService,
  zuoraService : ZuoraService,
  products:  Map[ProductRatePlan, String]) extends ActivityTracking {

  def start() = {
    memberRepository.salesforce.authTask.start()
    zuoraService.start()
  }

  val subscriptionService = new SubscriptionService(products, zuoraService)

  def updateDefaultCard(member: PaidMember, token: String): Future[Stripe.Card] = {
    for {
      customer <- stripeService.Customer.updateCard(member.stripeCustomerId, token)
      memberId <- memberRepository.upsert(member.identityId, Json.obj(Keys.DEFAULT_CARD_ID -> customer.card.id))
    } yield customer.card
  }

  def cancelSubscription(member: Member, user: IdMinimalUser, campaignCode: Option[String] = None): Future[String] = {
    for {
      subscription <- subscriptionService.cancelSubscription(member, member.tier == Tier.Friend)
    } yield {
      memberRepository.metrics.putCancel(member.tier)
      track(MemberActivity("cancelMembership", MemberData(member.salesforceContactId, member.identityId, member.tier.name, campaignCode = campaignCode)))(user)
      ""
    }
  }

  def downgradeSubscription(member: Member, user: IdMinimalUser, campaignCode: Option[String] = None): Future[String] = {
    for {
      _ <- subscriptionService.downgradeSubscription(member, FriendTierPlan)
    } yield {
      memberRepository.metrics.putDowngrade(member.tier)
      track(
        MemberActivity(
          "downgradeMembership",
          MemberData(
            member.salesforceContactId,
            member.identityId,
            member.tier.name,
            Some(DowngradeAmendment(member.tier)), //getting effective date and subscription annual / month is proving difficult
            campaignCode=campaignCode
          )))(user)

      ""
    }
  }

}
