package services

import com.gu.membership.salesforce.Member.Keys
import com.gu.membership.salesforce._
import com.gu.membership.stripe.{Stripe, StripeService}
import com.gu.monitoring.StatusMetrics
import configuration.Config
import model.{FriendTierPlan, IdMinimalUser, TierPlan}
import monitoring.TouchpointBackendMetrics
import play.api.libs.json.Json
import services.zuora.ZuoraService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object TouchpointBackend {

  def apply(touchpointBackendConfig: TouchpointBackendConfig): TouchpointBackend = {

    val stripeService = new StripeService(touchpointBackendConfig.stripe, new TouchpointBackendMetrics with StatusMetrics {
      val backendEnv = touchpointBackendConfig.stripe.envName
      val service = "Stripe"
    })

    val zuoraService = new ZuoraService(touchpointBackendConfig.zuora)

    val memberRepository = new FrontendMemberRepository(touchpointBackendConfig.salesforce)

    TouchpointBackend(memberRepository, stripeService, zuoraService)
  }

  val Normal = TouchpointBackend(Config.touchpointDefaultBackend)
  val TestUser = TouchpointBackend(Config.touchpointTestBackend)

  val All = Seq(Normal, TestUser)

  def forUser(user: IdMinimalUser) = if (user.isTestUser) TestUser else Normal
}

case class TouchpointBackend(
  memberRepository: FrontendMemberRepository,
  stripeService: StripeService,
  zuoraService : ZuoraService) {

  def start() = {
    memberRepository.salesforce.authTask.start()
    zuoraService.start()
  }

  val subscriptionService = new SubscriptionService(zuoraService.apiConfig.productRatePlans, zuoraService)

  def updateDefaultCard(member: PaidMember, token: String): Future[Stripe.Card] = {
    for {
      customer <- stripeService.Customer.updateCard(member.stripeCustomerId, token)
      memberId <- memberRepository.upsert(member.identityId, Json.obj(Keys.DEFAULT_CARD_ID -> customer.card.id))
    } yield customer.card
  }

  def cancelSubscription(member: Member): Future[String] = {
    for {
      subscription <- subscriptionService.cancelSubscription(member, member.tier == Tier.Friend)
    } yield {
      memberRepository.metrics.putCancel(member.tier)
      ""
    }
  }

  def downgradeSubscription(member: Member): Future[String] = {
    for {
      _ <- subscriptionService.downgradeSubscription(member, FriendTierPlan)
    } yield {
      memberRepository.metrics.putDowngrade(member.tier)
      ""
    }
  }

}
