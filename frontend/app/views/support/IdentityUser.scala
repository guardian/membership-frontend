package views.support

import com.gu.i18n.{Country, CountryGroup}
import com.gu.identity.model.{PrivateFields, PublicFields, StatusFields, User => IdUser}
import com.gu.memsub.Address
import utils.TestUsers

case class IdentityUser(publicFields: PublicFields, privateFields: PrivateFields, marketingChoices: StatusFields, passwordExists: Boolean, email: String) {
  private val countryName: Option[String] = privateFields.billingCountry orElse privateFields.country

  val country: Option[Country] =
    countryName.flatMap(CountryGroup.countryByNameOrCode)

  val countryGroup: Option[CountryGroup] =
    countryName.flatMap(CountryGroup.byCountryNameOrCode)

  val deliveryAddress = Address(
    lineOne =       privateFields.address1.mkString,
    lineTwo =       privateFields.address2.mkString,
    town =          privateFields.address3.mkString,
    countyOrState = privateFields.address4.mkString,
    postCode =      privateFields.postcode.mkString,
    countryName =   privateFields.country.mkString
  )

  val billingAddress = Address(
    lineOne =       privateFields.billingAddress1.mkString,
    lineTwo =       privateFields.billingAddress2.mkString,
    town =          privateFields.billingAddress3.mkString,
    countyOrState = privateFields.billingAddress4.mkString,
    postCode =      privateFields.billingPostcode.mkString,
    countryName =   privateFields.billingCountry.mkString
  )
  def displayName: Option[String] = publicFields.username.orElse(privateFields.firstName)
  def isTestUser: Boolean = privateFields.firstName.exists(TestUsers.isTestUser)
}

object IdentityUser {
  val BlankAddress = Address("", "", "", "", "", "")

  /**
  * An Identity user, for view purposes, with default empty private and status fields
  */
  def apply(user: IdUser, passwordExists: Boolean): IdentityUser =
    IdentityUser(
      publicFields = user.publicFields,
      privateFields = user.privateFields,
      marketingChoices = user.statusFields,
      passwordExists = passwordExists,
      email = user.primaryEmailAddress
    )
}
