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

    val membersLogo = SVGImage(
      "Guardian Membership",
      "images/logos/brand/guardian-members-1.svg",
      width=240, height=83
    )

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
