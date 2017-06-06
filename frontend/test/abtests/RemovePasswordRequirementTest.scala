package abtests

import com.gu.i18n.CountryGroup
import com.gu.identity.play.{IdUser, PublicFields}
import org.specs2.mutable.Specification
import views.support.IdentityUser
import RemovePasswordRequirement.userEligibleForTest
import com.gu.i18n.CountryGroup.UK

class RemovePasswordRequirementTest extends Specification {

  val idUser = IdUser("1","foo@gu.com", PublicFields(None), None, None)
  val UserWithPassword = IdentityUser(idUser, passwordExists = true)
  val UserWithNoPassword = IdentityUser(idUser, passwordExists = false)

  "user" should {
    "be eligible for the AB test if they have no password and are in the UK" in {
      userEligibleForTest(Some(UserWithNoPassword), UK) must beTrue
    }

    "not be eligible for the AB test if they have a password" in {
      forall(CountryGroup.allGroups) { cg =>
        userEligibleForTest(Some(UserWithPassword), cg) must beFalse
      }
    }

    "not be eligible for the AB test if they are not in the UK" in {
      forall(CountryGroup.allGroups.toSet - UK) { cg =>
        userEligibleForTest(Some(UserWithNoPassword), cg) must beFalse
      }
    }
  }

  "control variant" should {
    "always require password" in {
      forall(CountryGroup.allGroups) { cg =>
        RemovePasswordRequirement.control.requirePasswordFor(Some(UserWithNoPassword), cg) must beTrue
      }
    }
  }

  "new variant" should {
    "not require password for a user if they have no password and are in the UK" in {
      RemovePasswordRequirement.PasswordNotRequiredVariant.requirePasswordFor(Some(UserWithNoPassword), UK) must beFalse
    }
  }
}
