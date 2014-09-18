import configuration.Config
import services.zuora.ZuoraService

package object services {
  val StripeService = new StripeService(Config.stripeApiConfig)

  val ZuoraService = new ZuoraService(Config.zuoraApiConfig)

  val SubscriptionService = new SubscriptionService(Config.tierRatePlanIds, ZuoraService)
}
