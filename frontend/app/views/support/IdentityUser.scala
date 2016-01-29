package views.support

import com.gu.i18n.{Country, CountryGroup}
import com.gu.identity.model.PublicFields
import com.gu.identity.play.{StatusFields, PrivateFields}
import com.gu.identity.play.IdUser
import utils.TestUsers


case class IdentityUser(privateFields: PrivateFields, marketingChoices: StatusFields, passwordExists: Boolean) {
  private val countryName: Option[String] = privateFields.billingCountry orElse privateFields.country

  val country: Option[Country] =
    countryName.flatMap(CountryGroup.countryByNameOrCode)

  val countryGroup: Option[CountryGroup] =
    countryName.flatMap(CountryGroup.byCountryNameOrCode)

  def isTestUser: Boolean = privateFields.firstName.exists(TestUsers.isTestUser)
}

object IdentityUser {
  /**
  * An Identity user, for view purposes, with default empty private and status fields
  */
  def apply(user: IdUser, passwordExists: Boolean): IdentityUser =
    IdentityUser(
      privateFields = user.privateFields.getOrElse(PrivateFields()),
      marketingChoices = user.statusFields.getOrElse(StatusFields()),
      passwordExists = passwordExists
    )
}
