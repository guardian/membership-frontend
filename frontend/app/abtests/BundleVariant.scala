package abtests

import abtests.Distribution.{ADistribution, BDistribution}
import play.api.mvc.RequestHeader

import scala.util.Random

/**
  * Created by santiago_fernandez on 25/11/2016.
  */
object BundleVariant {
  val cookieName = "ab-bundle"

  val all = Seq[BundleVariant](A1, A2, A3, B1, B2, B3)

  def deriveFlowVariant(implicit request: RequestHeader): BundleVariant =
    getFlowVariantFromRequestCookie(request).getOrElse(BundleVariant.all(Random.nextInt(BundleVariant.all.size)))

  def getFlowVariantFromRequestCookie(request: RequestHeader): Option[BundleVariant] = for {
    cookieValue <- request.cookies.get(cookieName)
    variant <- BundleVariant.lookup(cookieValue.value)
    } yield variant

  case object A1 extends BundleVariant {
    override val testId: String = "test-A1"
    override val prices: Map[UserLayer, Double] = Map((Supporter, 6.5), (DigitalPack, 14), (Saturday, 16), (GuardianWeekly, 24), (SatGW,24))
    override val distribution: Distribution = ADistribution
  }

  case object A2 extends BundleVariant {
    override val testId: String = "test-A2"
    override val prices: Map[UserLayer, Double] = Map()
    override val distribution: Distribution = ADistribution
  }

  case object A3 extends BundleVariant {
    override val testId: String = "test-A3"
    override val prices: Map[UserLayer, Double] = Map()
    override val distribution: Distribution = ADistribution
  }

  case object B1 extends BundleVariant {
    override val testId: String = "test-B1"
    override val prices: Map[UserLayer, Double] = Map()
    override val distribution: Distribution = BDistribution
  }

  case object B2 extends BundleVariant {
    override val testId: String = "test-B2"
    override val prices: Map[UserLayer, Double] = Map()
    override val distribution: Distribution = BDistribution
  }

  case object B3 extends BundleVariant {
    override val testId: String = "test-B3"
    override val prices: Map[UserLayer, Double] = Map()
    override val distribution: Distribution = BDistribution
  }

  def lookup(name: String): Option[BundleVariant] = all.find(_.testId == name)
}

sealed trait BundleVariant {
  val testId: String
  val prices: Map[UserLayer, Double]
  val distribution: Distribution
}

sealed trait UserLayer

case object Supporter extends UserLayer
case object DigitalPack extends UserLayer
case object Saturday extends UserLayer
case object GuardianWeekly extends UserLayer
case object Weekend extends UserLayer
case object SatGW extends UserLayer

sealed trait Feature

case object Support extends Feature
case object Commenting extends Feature
case object AdFreeApp extends Feature
case object AdFreeWeb extends Feature
case object DailyEdition extends Feature
case object Newspaper extends Feature

object Distribution {
  case object ADistribution extends Distribution {
    override val elements: Map[UserLayer, Seq[Feature]] = Map((Supporter, Seq(Support, Commenting, AdFreeApp, AdFreeWeb)),
      (DigitalPack, Seq(Support, Commenting, AdFreeApp, AdFreeWeb, DailyEdition)), (Saturday, Seq(Support, Commenting, AdFreeApp, AdFreeWeb, DailyEdition, Newspaper)))
  }

  case object BDistribution extends Distribution {
    override val elements: Map[UserLayer, Seq[Feature]] = Map((Supporter, Seq(Support, Commenting, AdFreeApp, AdFreeWeb)),
      (DigitalPack, Seq(Support, Commenting, AdFreeApp, AdFreeWeb, DailyEdition)), (Saturday, Seq(Support, Commenting, AdFreeApp, AdFreeWeb, DailyEdition, Newspaper)))
  }
}

sealed trait Distribution {
  val elements: Map[UserLayer, Seq[Feature]]
}



