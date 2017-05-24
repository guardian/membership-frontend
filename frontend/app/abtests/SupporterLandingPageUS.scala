package abtests

import abtests.AudienceRange.FullAudience

case object SupporterLandingPageUSA extends ABTest("supporter-landing-page-usa", FullAudience, _.path.startsWith("/us/supporter")) {

  case class Variant(slug: String, showNewDesign: Boolean) extends BaseVariant

  val variants = Seq(
    Variant("control", showNewDesign = false),
    Variant("new-design",  showNewDesign = true)
  )
}
