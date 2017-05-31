package abtests

import abtests.AudienceRange.FullAudience
import views.support.IdentityUser

case object RemovePasswordRequirement extends ABTest("remove-password-requirement", FullAudience) {

  case class Variant(slug: String, requireGuardianPasswordForSocialSignInUsers: Boolean) extends BaseVariant {
    def requirePasswordFor(identityUser: Option[IdentityUser]) =
      identityUser.map(u => requireGuardianPasswordForSocialSignInUsers && !u.passwordExists).getOrElse(true)
  }

  val variants = Seq(
    Variant("control", requireGuardianPasswordForSocialSignInUsers = true),
    Variant("password-not-required",  requireGuardianPasswordForSocialSignInUsers = false)
  )
}
