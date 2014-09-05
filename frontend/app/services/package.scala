import configuration.Config

package object services {
  val StripeService = new StripeService(Config.stripeApiConfig)
}
