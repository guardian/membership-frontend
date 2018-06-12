package model

import views.support.Asset
import play.api.libs.json._

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
      s"images/logos/brand/guardian-members.svg",
      width=300, height=90
    )

    val guardianLogo = SVGImage(
      "Guardian Logo",
      s"images/logos/brand/guardian-logo.svg",
      width=320, height=60
    )

    val membersLogoTightcrop = SVGImage(
      "Guardian Membership",
      s"images/logos/brand/guardian-members-tightcrop.svg",
      width=300, height=90
    )

    val guardianLiveHeader = SVGImage(
      "Guardian Live",
      "images/providers/guardian-live-header.svg",
      width = 478, height = 68
    )

    val guardianMasterclassesHeader = SVGImage(
      "Guardian Masterclasses",
      "images/providers/masterclasses-header.svg",
      width = 789, height = 68
    )

    val guardianPatronsHeader = SVGImage(
      "TheGuardian Patrons",
      "images/logos/guardian-patrons.svg",
      width = 789, height = 68
    )

    val guardianTitlePiece = SVGImage(
      "Guardian TitlePiece",
      s"images/logos/brand/guardian-titlepiece.svg",
      width = 160, height = 30
    )

    val guardianRoundel = SVGImage(
      "Guardian Roundel",
      s"images/logos/brand/guardian-roundel-cy.svg",
      width = 52, height = 52
    )
  }

}
