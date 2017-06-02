package abtests

import abtests.AudienceRange.FullAudience
import com.gu.i18n.CountryGroup
import views.support.IdentityUser

case object RemovePasswordRequirement extends ABTest("remove-ss-password-requirement", FullAudience) {

  case class Variant(slug: String, requireGuardianPasswordForSocialSignInUsers: Boolean) extends BaseVariant {
    def requirePasswordFor(identityUser: Option[IdentityUser], cg: CountryGroup) = identityUser match {
      case None => true
      case Some(user) =>
        val canWaivePasswordRequirement = (cg == CountryGroup.UK) && !requireGuardianPasswordForSocialSignInUsers
        !(user.passwordExists || canWaivePasswordRequirement)
    }
  }

  val variants = Seq(
    Variant("control", requireGuardianPasswordForSocialSignInUsers = true),
    Variant("password-not-required",  requireGuardianPasswordForSocialSignInUsers = false)
  )
}
