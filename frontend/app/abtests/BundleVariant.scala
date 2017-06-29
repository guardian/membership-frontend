package abtests

import abtests.BundleTier._

object BundleVariant {

  import Distribution._

  val cookieName = "ab-bundle"

  val all = Seq[BundleVariant](
    BundleVariant(CONTROL, Map((Supporter, 6.5), (DigitalPack, 12), (Saturday, 18), (GuardianWeekly, 18), (Weekend, 25), (SatGW, 25), (SixDay, 45), (SevenDay, 50))),
    BundleVariant(VARIANT, Map((Supporter, 6.5), (DigitalPack, 12), (Saturday, 18), (GuardianWeekly, 18), (Weekend, 25), (SatGW, 25), (SixDay, 45), (SevenDay, 50)), hasAdFree = false),
    BundleVariant(PDCPRE, Map((Supporter, 6.5), (DigitalPack, 12), (Saturday, 18), (GuardianWeekly, 18), (Weekend, 25), (SatGW, 25), (SixDay, 45), (SevenDay, 50)), hasPaidComments = true, hasContributions = false),
    BundleVariant(PDCPOST, Map((Supporter, 6.5), (DigitalPack, 12), (Saturday, 18), (GuardianWeekly, 18), (Weekend, 25), (SatGW, 25), (SixDay, 45), (SevenDay, 50)), hasPaidComments = true, hasContributions = false),
    BundleVariant(DIGIPRICE1A, Map((Supporter, 6.5), (DigitalPack, 11.99), (Saturday, 18), (GuardianWeekly, 18), (Weekend, 25), (SatGW, 25), (SixDay, 45), (SevenDay, 50)), hasAdFree = true, hasPaidComments = false, hasContributions = true, hasDigital = true, hasPrintAndDigital = false),
    BundleVariant(DIGIPRICE1B, Map((Supporter, 6.5), (DigitalPack, 14.99), (Saturday, 18), (GuardianWeekly, 18), (Weekend, 25), (SatGW, 25), (SixDay, 45), (SevenDay, 50)), hasAdFree = true, hasPaidComments = false, hasContributions = true, hasDigital = true, hasPrintAndDigital = false),
    BundleVariant(DIGIPRICE1C, Map((Supporter, 6.5), (DigitalPack, 9.99), (Saturday, 18), (GuardianWeekly, 18), (Weekend, 25), (SatGW, 25), (SixDay, 45), (SevenDay, 50)), hasAdFree = true, hasPaidComments = false, hasContributions = true, hasDigital = true, hasPrintAndDigital = false),
    BundleVariant(DIGIPRICE1TA, Map((Supporter, 6.5), (DigitalPack, 11.99), (Saturday, 18), (GuardianWeekly, 18), (Weekend, 25), (SatGW, 25), (SixDay, 45), (SevenDay, 50)), hasAdFree = true, hasPaidComments = false, hasContributions = true, hasDigital = true, hasPrintAndDigital = false),
    BundleVariant(DIGIPRICE1TB, Map((Supporter, 6.5), (DigitalPack, 14.99), (Saturday, 18), (GuardianWeekly, 18), (Weekend, 25), (SatGW, 25), (SixDay, 45), (SevenDay, 50)), hasAdFree = true, hasPaidComments = false, hasContributions = true, hasDigital = true, hasPrintAndDigital = false),
    BundleVariant(DIGIPRICE1TC, Map((Supporter, 6.5), (DigitalPack, 9.99), (Saturday, 18), (GuardianWeekly, 18), (Weekend, 25), (SatGW, 25), (SixDay, 45), (SevenDay, 50)), hasAdFree = true, hasPaidComments = false, hasContributions = true, hasDigital = true, hasPrintAndDigital = false),
    BundleVariant(DIGIPRICE1BA, Map((Supporter, 6.5), (DigitalPack, 11.99), (Saturday, 18), (GuardianWeekly, 18), (Weekend, 25), (SatGW, 25), (SixDay, 45), (SevenDay, 50)), hasAdFree = true, hasPaidComments = false, hasContributions = true, hasDigital = true, hasPrintAndDigital = false),
    BundleVariant(DIGIPRICE1BB, Map((Supporter, 6.5), (DigitalPack, 14.99), (Saturday, 18), (GuardianWeekly, 18), (Weekend, 25), (SatGW, 25), (SixDay, 45), (SevenDay, 50)), hasAdFree = true, hasPaidComments = false, hasContributions = true, hasDigital = true, hasPrintAndDigital = false),
    BundleVariant(DIGIPRICE1BC, Map((Supporter, 6.5), (DigitalPack, 9.99), (Saturday, 18), (GuardianWeekly, 18), (Weekend, 25), (SatGW, 25), (SixDay, 45), (SevenDay, 50)), hasAdFree = true, hasPaidComments = false, hasContributions = true, hasDigital = true, hasPrintAndDigital = false)
  )

  def lookup(name: String): Option[BundleVariant] = {
    all.find(_.testId == name)
  }
}

case class BundleVariant(distribution: Distribution,
                         prices: Map[BundleTier, Double],
                         hasAdFree: Boolean = true,
                         hasPaidComments: Boolean = false,
                         hasContributions: Boolean = true,
                         hasDigital: Boolean = true,
                         hasPrintAndDigital: Boolean = true) {
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
  // epic variants
  val DIGIPRICE1A = Distribution("BUNDLE_PRICE_TEST_1M_E_UK_A")
  val DIGIPRICE1B = Distribution("BUNDLE_PRICE_TEST_1M_E_UK_B")
  val DIGIPRICE1C = Distribution("BUNDLE_PRICE_TEST_1M_E_UK_C")
  // thrasher variants
  val DIGIPRICE1TA = Distribution("BUNDLE_PRICE_TEST_1M_T_UK_A")
  val DIGIPRICE1TB = Distribution("BUNDLE_PRICE_TEST_1M_T_UK_B")
  val DIGIPRICE1TC = Distribution("BUNDLE_PRICE_TEST_1M_T_UK_C")
  // banner variants
  val DIGIPRICE1BA = Distribution("BUNDLE_PRICE_TEST_1M_B_UK_A")
  val DIGIPRICE1BB = Distribution("BUNDLE_PRICE_TEST_1M_B_UK_B")
  val DIGIPRICE1BC = Distribution("BUNDLE_PRICE_TEST_1M_B_UK_C")
}

case class Distribution(name: String)



