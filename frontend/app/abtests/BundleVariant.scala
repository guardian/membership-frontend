package abtests

import abtests.BundleTier._

object BundleVariant {

  import Distribution._

  val cookieName = "ab-bundle"

  val all = Seq[BundleVariant](
    BundleVariant(CONTROL, Map((Supporter, 6.5), (DigitalPack, 12), (Saturday, 18), (GuardianWeekly, 18), (Weekend, 25), (SatGW, 25), (SixDay, 45), (SevenDay, 50))),
    BundleVariant(VARIANT, Map((Supporter, 6.5), (DigitalPack, 12), (Saturday, 18), (GuardianWeekly, 18), (Weekend, 25), (SatGW, 25), (SixDay, 45), (SevenDay, 50)), hasAdFree = false),
    BundleVariant(PDCPRE, Map((Supporter, 6.5), (DigitalPack, 12), (Saturday, 18), (GuardianWeekly, 18), (Weekend, 25), (SatGW, 25), (SixDay, 45), (SevenDay, 50)), hasPaidComments = true, hasContributions = false),
    BundleVariant(PDCPOST, Map((Supporter, 6.5), (DigitalPack, 12), (Saturday, 18), (GuardianWeekly, 18), (Weekend, 25), (SatGW, 25), (SixDay, 45), (SevenDay, 50)), hasPaidComments = true, hasContributions = false)
  )

  def lookup(name: String): Option[BundleVariant] = {
    all.find(_.testId == name)
  }
}

case class BundleVariant(distribution: Distribution,
                         prices: Map[BundleTier, Double],
                         hasAdFree: Boolean = true,
                         hasPaidComments: Boolean = false,
                         hasContributions: Boolean = true) {
  val testId =  distribution.name

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
  val CONTROL = Distribution("MEMBERSHIP_A_ADX_THRASHER_UK_CONTROL")
  val VARIANT = Distribution("MEMBERSHIP_A_ADX_THRASHER_UK_VARIANT")
  val PDCPRE = Distribution("MEMBERSHIP_A_PDCOM_PRE")
  val PDCPOST = Distribution("MEMBERSHIP_A_PDCOM_POST")
}

case class Distribution(name: String)



