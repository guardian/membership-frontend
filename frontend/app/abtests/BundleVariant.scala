package abtests

import play.api.mvc.RequestHeader

import scala.util.Random

object BundleVariant {

  import Distribution._

  val cookieName = "ab-bundle"

  val all = Seq[BundleVariant](
    BundleVariant(A, 1, Map((Supporter, 6.5), (DigitalPack, 14), (Saturday, 16), (GuardianWeekly, 24), (SatGW, 24))),
    BundleVariant(A, 2, Map((Supporter, 6.5), (DigitalPack, 14), (Saturday, 16), (GuardianWeekly, 24), (SatGW, 24))),
    BundleVariant(A, 3, Map((Supporter, 6.5), (DigitalPack, 14), (Saturday, 16), (GuardianWeekly, 24), (SatGW, 24))),
    BundleVariant(B, 1, Map((Supporter, 6.5), (DigitalPack, 14), (Saturday, 16), (GuardianWeekly, 24), (SatGW, 24))),
    BundleVariant(B, 2, Map((Supporter, 6.5), (DigitalPack, 14), (Saturday, 16), (GuardianWeekly, 24), (SatGW, 24))),
    BundleVariant(B, 3, Map((Supporter, 6.5), (DigitalPack, 14), (Saturday, 16), (GuardianWeekly, 24), (SatGW, 24)))
  )

  def deriveFlowVariant(implicit request: RequestHeader): BundleVariant =
    getFlowVariantFromRequestCookie(request).getOrElse(BundleVariant.all(Random.nextInt(BundleVariant.all.size)))

  def getFlowVariantFromRequestCookie(request: RequestHeader): Option[BundleVariant] = for {
    cookieValue <- request.cookies.get(cookieName)
    variant <- BundleVariant.lookup(cookieValue.value)
  } yield variant

  def lookup(name: String): Option[BundleVariant] = all.find(_.testId == name)
}

case class BundleVariant(distribution: Distribution, priceIndex: Int, prices: Map[BundleTier, Double]) {
  val testId = distribution.name + priceIndex
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



