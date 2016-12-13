package configuration

import com.typesafe.config.ConfigFactory
import play.twirl.api.Html
import scala.collection.JavaConversions._

sealed trait LandingCopy {
  val mainTitle: Seq[String]
  val tagLine: String
  val preVideo: Seq[String]
  val postVideo: Seq[String]
  val benefitsTitle: Seq[String]
  val benefitsIntro: Seq[String]
  val benefits: Seq[String]
  val benefitsOutro: Seq[String]
  val whySupportTitle: Seq[String]
  val whySupport: Seq[String]
}


object LandingCopy {

  val config = ConfigFactory.load("landingCopy.conf.json")

  case object ukLandingCopy extends LandingCopy {
    override val mainTitle = config.getStringList("uk.mainTitle").toList
    override val tagLine = config.getString("uk.tagLine")
    override val preVideo = config.getStringList("uk.preVideo").toList
    override val postVideo = config.getStringList("uk.postVideo").toList
    override val benefitsTitle = config.getStringList("uk.benefitsTitle").toList
    override val benefitsIntro = config.getStringList("uk.benefitsIntro").toList
    override val benefits = config.getStringList("uk.benefits").toList
    override val benefitsOutro = config.getStringList("uk.benefitsOutro").toList
    override val whySupportTitle = config.getStringList("uk.whySupportTitle").toList
    override val whySupport = config.getStringList("uk.whySupport").toList
  }
}


