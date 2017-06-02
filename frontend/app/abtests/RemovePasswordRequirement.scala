package abtests

import abtests.AudienceRange.FullAudience
import com.gu.i18n.CountryGroup
import views.support.IdentityUser

case object RemovePasswordRequirement extends ABTest("remove-ss-password-requirement", FullAudience) {

  val EligibilitySessionKey = s"$abSlug-eligible"

  def userEligibleForTest(identityUser: IdentityUser, cg: CountryGroup) = (cg == CountryGroup.UK) && !identityUser.passwordExists

  case class Variant(slug: String, requireGuardianPasswordForSocialSignInUsers: Boolean) extends BaseVariant {

    def requirePasswordFor(identityUser: Option[IdentityUser], cg: CountryGroup) = identityUser match {
      case None => true
      case Some(user) => userEligibleForTest(user, cg) && !requireGuardianPasswordForSocialSignInUsers
    }
  }

  val variants = Seq(
    Variant("control", requireGuardianPasswordForSocialSignInUsers = true),
    Variant("password-not-required",  requireGuardianPasswordForSocialSignInUsers = false)
  )
}
