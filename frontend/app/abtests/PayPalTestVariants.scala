package abtests

import scala.util.Random

object PayPalTestVariants {
  val withPayPal = "with_paypal"
  val stripeOnly = "stripe_only"
  val variants = List(withPayPal, stripeOnly)
  def allocateVariant() = variants(Random.nextInt(2))
}
