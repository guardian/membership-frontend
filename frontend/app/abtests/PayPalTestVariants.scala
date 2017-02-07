package abtests

import scala.util.Random

object PayPalTestVariants {
  val variants = List("with_paypal", "stripe_only")
  def allocateVariant() = variants(Random.nextInt(2))
}
