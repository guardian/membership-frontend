package views.support

import com.gu.i18n.CountryGroup
import com.gu.identity.play.{StatusFields, PrivateFields}
import com.gu.identity.play.IdUser

/**
  * A Identity user, for view purposes, with default empty private and status fields
  */
case class IdentityUser(privateFields: PrivateFields, marketingChoices: StatusFields, passwordExists: Boolean) {
  def withCountryGroup(countryGroup: CountryGroup): IdentityUser = {
    val country = privateFields.country.orElse(countryGroup.defaultCountry.map(_.alpha2))
    copy(
      privateFields = privateFields.copy(
        billingCountry = country
      )
    )
  }
}

object IdentityUser {
  def apply(user: IdUser, passwordExists: Boolean): IdentityUser =
    IdentityUser(
      privateFields = user.privateFields.getOrElse(PrivateFields()),
      marketingChoices = user.statusFields.getOrElse(StatusFields()),
      passwordExists = passwordExists
    )
}
