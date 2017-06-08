package abtests

import abtests.AudienceRange.FullAudience
import com.gu.i18n.CountryGroup
import views.support.IdentityUser

case object RemovePasswordRequirement extends ABTest("ss-no-password", FullAudience) {

  val EligibilitySessionKey = s"$abSlug-eligible"

  def userEligibleForTest(identityUser: Option[IdentityUser], cg: CountryGroup) = identityUser match {
    case None => false
    case Some(user) => (cg == CountryGroup.UK) && !user.passwordExists
  }


  case class Variant(slug: String, waivePasswordRequirement: Boolean) extends BaseVariant {

    def requirePasswordFor(identityUser: Option[IdentityUser], cg: CountryGroup) = {
      val userHasPassword = identityUser.exists(_.passwordExists)
      val userCanWaivePasswordRequirement = userEligibleForTest(identityUser, cg) && waivePasswordRequirement

      !userHasPassword && !userCanWaivePasswordRequirement
    }

  }

  val PasswordNotRequiredVariant = Variant("password-not-required", waivePasswordRequirement = true)

  val variants = Seq(
    Variant("control", waivePasswordRequirement = false),
    PasswordNotRequiredVariant
  )
}
