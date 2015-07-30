package model

import views.support.Asset

object SVG {

  case class SVGImage(
    label: String,
    path: String,
    width: Int,
    height: Int
  ) {
    val src = Asset.at(path)
  }

  object Logos {
    val membershipLogo = SVGImage(
      "Guardian Membership",
      "images/logos/membership-logo.svg",
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
