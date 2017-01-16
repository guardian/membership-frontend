package abtests

import play.api.mvc.RequestHeader

import scala.util.Random

object BundleVariant {

  import Distribution._

  val cookieName = "ab-bundle"

  val all = Seq[BundleVariant](
    BundleVariant(A, 1, Map((Supporter, 6.5), (DigitalPack, 14), (Saturday, 16), (GuardianWeekly, 24), (Weekend, 24), (SatGW, 24))),
    BundleVariant(A, 2, Map((Supporter, 7.5), (DigitalPack, 14), (Saturday, 18), (GuardianWeekly, 18), (Weekend, 25), (SatGW, 25))),
    BundleVariant(A, 3, Map((Supporter, 8), (DigitalPack, 15), (Saturday, 20), (GuardianWeekly, 20), (Weekend, 30), (SatGW, 30))),
    BundleVariant(B, 1, Map((Supporter, 6), (DigitalPack, 10), (Saturday, 16), (GuardianWeekly, 16), (Weekend, 24), (SatGW, 24))),
    BundleVariant(B, 2, Map((Supporter, 6.5), (DigitalPack, 12), (Saturday, 18), (GuardianWeekly, 18), (Weekend, 25), (SatGW, 25))),
    BundleVariant(B, 3, Map((Supporter, 7), (DigitalPack, 14), (Saturday, 20), (GuardianWeekly, 20), (Weekend, 30), (SatGW, 24)))
  )

  def deriveFlowVariant(implicit request: RequestHeader): BundleVariant =
    getFlowVariantFromRequestCookie(request).getOrElse(BundleVariant.all(Random.nextInt(BundleVariant.all.size)))

  def getFlowVariantFromRequestCookie(request: RequestHeader): Option[BundleVariant] = for {
    cookieValue <- request.cookies.get(cookieName)
    variant <- BundleVariant.lookup(cookieValue.value)
  } yield variant

  def lookup(name: String): Option[BundleVariant] = {
    printf(name)
    all.find(_.testId == name)
  }
}

case class BundleVariant(distribution: Distribution, priceIndex: Int, prices: Map[BundleTier, Double]) {
  val testId =  "MEMBERSHIP_AB_THRASHER_UK_" + distribution.name + priceIndex
}

sealed trait BundleTier

case object Supporter extends BundleTier

case object DigitalPack extends BundleTier

case object Saturday extends BundleTier

case object GuardianWeekly extends BundleTier

case object Weekend extends BundleTier

case object SatGW extends BundleTier


object Distribution {
  val A = Distribution("A")

  val B = Distribution("B")
}

case class Distribution(name: String)



