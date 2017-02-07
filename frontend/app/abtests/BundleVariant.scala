package abtests

import abtests.BundleTier._

object BundleVariant {

  import Distribution._

  val cookieName = "ab-bundle"

  val all = Seq[BundleVariant](
    BundleVariant(CONTROL, Map((Supporter, 6.5), (DigitalPack, 12), (Saturday, 18), (GuardianWeekly, 18), (Weekend, 25), (SatGW, 25), (SixDay, 45), (SevenDay, 50))),
    BundleVariant(VARIANT, Map((Supporter, 6.5), (DigitalPack, 12), (Saturday, 18), (GuardianWeekly, 18), (Weekend, 25), (SatGW, 25), (SixDay, 45), (SevenDay, 50)), hasAdFree = false)
  )

  def lookup(name: String): Option[BundleVariant] = {
    all.find(_.testId == name)
  }
}

case class BundleVariant(distribution: Distribution, prices: Map[BundleTier, Double], hasAdFree: Boolean = true) {
  val testId =  s"MEMBERSHIP_A_ADS_THRASHER_UK_${distribution.name}"

  def prettyMonthlyPrice(tier: BundleTier) = f"£${prices(tier)}%.2f/month"
}

sealed trait BundleTier

object BundleTier {

  case object Supporter extends BundleTier

  case object DigitalPack extends BundleTier

  case object Saturday extends BundleTier

  case object GuardianWeekly extends BundleTier

  case object Weekend extends BundleTier

  case object SatGW extends BundleTier

  case object SixDay extends BundleTier

  case object SevenDay extends BundleTier

}


object Distribution {
  val CONTROL = Distribution("CONTROL")
  val VARIANT = Distribution("VARIANT")
}

case class Distribution(name: String)



