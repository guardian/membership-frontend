package services

import com.gu.membership.salesforce.{Authentication, MemberRepository, Scalaforce}
import configuration.Config
import services.zuora.ZuoraService
import utils.ScheduledTask

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

}