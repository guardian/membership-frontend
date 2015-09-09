package model

import views.support.Asset
import play.api.libs.json._
import scala.util.Random

object SVG {

  case class SVGImage(
    label: String,
    path: String,
    width: Int,
    height: Int
  ) {
    val src = Asset.at(path)
  }

  implicit val imageWrites = new Writes[SVGImage] {
    def writes(image: SVGImage) = Json.obj(
      "label" -> image.label,
      "src" -> image.src,
      "width" -> image.width,
      "height" -> image.height
    )
  }

  object Logos {

    val membersLogos = for(index <- 1 to 4) yield SVGImage(
      "Guardian Membership",
      s"images/logos/brand/guardian-members-$index.svg",
      width=300, height=90
    )

    def getRandomLogo = Random.shuffle(membersLogos.toList).head
    val membersLogo = getRandomLogo

    val guardianLive = SVGImage(
      "Guardian Live",
      "images/providers/guardian-live.svg",
      width=62, height=90
    )

    val guardianMasterclasses = SVGImage(
      "Guardian Masterclasses",
      "images/providers/masterclasses.svg",
      width=242, height=88
    )
  }

}
