package abtests

import abtests.AudienceRange.FullAudience

case object MergedRegistration extends ABTest("merged-registration",FullAudience) {

  case class Variant(slug: String, canWaiveAuth: Boolean) extends BaseVariant

  val variants = Seq(
    Variant("control", canWaiveAuth = false),
    Variant("merged",  canWaiveAuth = true)
  )
}
