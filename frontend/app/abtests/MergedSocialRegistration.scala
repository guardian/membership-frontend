package abtests

import abtests.AudienceRange.FullAudience
import services.AuthenticationService.authenticatedIdUserProvider

case object MergedSocialRegistration extends ABTest(
  "merged-social-registration",
  FullAudience,
  req => req.path.startsWith("/join/supporter/enter-details") && authenticatedIdUserProvider(req).isEmpty
) {

  case class Variant(slug: String, canWaiveAuth: Boolean) extends BaseVariant

  val variants = Seq(
    Variant("control", canWaiveAuth = false),
    Variant("merged",  canWaiveAuth = true)
  )
}
