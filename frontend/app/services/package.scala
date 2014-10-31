import configuration.Config
import services.zuora.ZuoraService

package object services {
  val StripeService = new StripeService(Config.touchpointBackendConfig.stripe)

  val ZuoraService = new ZuoraService(Config.touchpointBackendConfig.zuora)

  val SubscriptionService = new SubscriptionService(Config.touchpointBackendConfig.zuora.tierRatePlanIds, ZuoraService)
}
