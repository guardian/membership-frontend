package abtests

import scala.util.Random

object PayPalTestVariants {
  val withPayPal = "with_paypal"
  val stripeOnly = "stripe_only"
  val variants = List(withPayPal, stripeOnly)
  def allocateVariant() = None //Some(variants(Random.nextInt(2))) //uncomment to go live
}
