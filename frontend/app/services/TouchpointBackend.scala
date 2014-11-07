package services

import actions.RichUser
import com.gu.identity.model.User
import com.gu.membership.salesforce.Member.Keys
import com.gu.membership.salesforce._
import configuration.Config
import model.Stripe.Card
import model.{FriendTierPlan, TierPlan}
import monitoring.MemberMetrics
import services.zuora.ZuoraService
import utils.ScheduledTask

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

object TouchpointBackend {

  def apply(touchpointBackendConfig: TouchpointBackendConfig): TouchpointBackend = {
    val stripeService = new StripeService(touchpointBackendConfig.stripe)

    val zuoraService = new ZuoraService(touchpointBackendConfig.zuora)

    val memberRepository = new MemberRepository with ScheduledTask[Authentication] {
      val initialValue = Authentication("", "")
      val initialDelay = 0.seconds
      val interval = 30.minutes

      def refresh() = salesforce.getAuthentication

      val salesforce = new Scalaforce {
        val config = touchpointBackendConfig.salesforce

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

    memberRepository.start()

    TouchpointBackend(memberRepository, stripeService, zuoraService)
  }

  val Normal = TouchpointBackend(Config.touchpointBackendConfig)
  val TestUser = TouchpointBackend(Config.touchpointBackendConfig)

  val All = Seq(Normal, TestUser)

  def forUser(user: User) = if (user.isTestUser) TestUser else Normal
}

case class TouchpointBackend(
  memberRepository: MemberRepository,
  stripeService: StripeService,
  zuoraService : ZuoraService) {

  def start() = {
    // TODO start the MemberRepository here too
    zuoraService.start()
  }

  val subscriptionService = new SubscriptionService(zuoraService.apiConfig.tierRatePlanIds, zuoraService)

  def updateDefaultCard(member: PaidMember, token: String): Future[Card] = {
    for {
      customer <- stripeService.Customer.updateCard(member.stripeCustomerId, token)
      memberId <- memberRepository.upsert(member.identityId, Map(Keys.DEFAULT_CARD_ID -> customer.card.id))
    } yield customer.card
  }

  def cancelSubscription(member: Member): Future[String] = {
    for {
      subscription <- subscriptionService.cancelSubscription(member.salesforceAccountId, member.tier == Tier.Friend)
    } yield {
      MemberMetrics.putCancel(member.tier)
      ""
    }
  }

  def downgradeSubscription(member: Member, tierPlan: TierPlan): Future[String] = {
    for {
      _ <- subscriptionService.downgradeSubscription(member.salesforceAccountId, FriendTierPlan)
    } yield {
      MemberMetrics.putDowngrade(tierPlan.tier)
      ""
    }
  }

}