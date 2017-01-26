package abtests

import abtests.BundleTier._

object BundleVariant {

  import Distribution._

  val cookieName = "ab-bundle"

  val all = Seq[BundleVariant](
    BundleVariant(A, 1, Map((Supporter, 6.5), (DigitalPack, 12), (Saturday, 18), (GuardianWeekly, 18), (Weekend, 25), (SatGW, 25), (SixDay, 45), (SevenDay, 50))),
    BundleVariant(B, 1, Map((Supporter, 7.5), (DigitalPack, 14), (Saturday, 18), (GuardianWeekly, 18), (Weekend, 25), (SatGW, 25), (SixDay, 45), (SevenDay, 50))),
    BundleVariant(B, 2, Map((Supporter, 7.5), (DigitalPack, 14), (Saturday, 18), (GuardianWeekly, 18), (Weekend, 25), (SatGW, 25), (SixDay, 45), (SevenDay, 50)))
  )

  def lookup(name: String): Option[BundleVariant] = {
    all.find(_.testId == name)
  }
}

case class BundleVariant(distribution: Distribution, priceIndex: Int, prices: Map[BundleTier, Double]) {
  val testId =  s"MEMBERSHIP_AB_THRASHER_UK_${distribution.name}$priceIndex"

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
  val A = Distribution("A")
  val B = Distribution("B")
}

case class Distribution(name: String)



