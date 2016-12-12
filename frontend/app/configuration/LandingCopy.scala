package configuration

import com.typesafe.config.ConfigFactory
import play.twirl.api.Html
import scala.collection.JavaConversions._

sealed trait LandingCopy {
  val preVideo: Seq[String]
  val postVideo: Seq[String]
  val introBenefits: Seq[String]
  val benefits: Seq[String]
  val outBenefits: Seq[String]
  val whySupport: Seq[String]
}

object LandingCopy {

  val config = ConfigFactory.load("landingCopy.conf.json")

  case object ukLandingCopy extends LandingCopy {
    override val preVideo = config.getStringList("uk.preVideo").toList
    override val postVideo = config.getStringList("uk.postVideo").toList
    override val introBenefits = config.getStringList("uk.introBenefits").toList
    override val benefits = config.getStringList("uk.benefits").toList
    override val outBenefits = config.getStringList("uk.outBenefits").toList
    override val whySupport = config.getStringList("uk.whySupport").toList
  }
}


