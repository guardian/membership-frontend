package forms

import org.specs2.mutable.Specification

import play.api.data.Form

import com.gu.membership.zuora.Countries

import MemberForm._

class MemberFormTest extends Specification {
  val friendAddressForm = Form { friendAddressMapping }
  val paidAddressForm = Form { paidAddressMapping }

  def address(lineOne: String, lineTwo: String, town: String, countyOrState: String, postCode: String,
              country: String): Map[String, String] = {
    Map(
      "lineOne" -> lineOne,
      "lineTwo" -> lineTwo,
      "town" -> town,
      "countyOrState" -> countyOrState,
      "postCode" -> postCode,
      "country" -> country
    )
  }

  "AddressForm" should {
    "always require a country and postcode" in {
      friendAddressForm.bind(address("a", "b", "c", "d", "N1 9GU", "GB")).hasErrors must beFalse
      friendAddressForm.bind(address("a", "b", "c", "d", "", "")).hasErrors must beTrue

      paidAddressForm.bind(address("a", "b", "c", "d", "N1 9GU", "GB")).hasErrors must beFalse
      paidAddressForm.bind(address("a", "b", "c", "d", "", "")).hasErrors must beTrue
    }

    "also require line one and town for paid members" in {
      paidAddressForm.bind(address("a", "b", "c", "d", "N1 9GU", "GB")).hasErrors must beFalse
      paidAddressForm.bind(address("", "b", "", "d", "N1 9GU", "GB")).hasErrors must beTrue
    }

    "allow empty state for friends for any country except US/Canada" in {
      val form = friendAddressForm.bind(address("Kings Place", "90 York Way", "London", "", "N1 9GU", "GB"))
      form.hasErrors must beFalse
    }

    "allow empty state for paid members for any country except US/Canada" in {
      val form = paidAddressForm.bind(address("Kings Place", "90 York Way", "London", "", "N1 9GU", "GB"))
      form.hasErrors must beFalse
    }

    "require a valid state for friends when country is US" in {
      val badState = friendAddressForm.bind(address("The Guardian", "536 Broadway", "New York", "Greater York", "10012", "US"))
      badState.hasErrors must beTrue

      val goodState = friendAddressForm.bind(address("The Guardian", "536 Broadway", "New York", "New York", "10012", "US"))
      goodState.hasErrors must beFalse
    }

    "require a valid province for friends when country is Canada" in {
      val badProvince = friendAddressForm.bind(address("80 Elgin St", "", "Ottawa", "Oldfoundland", "K1P 5K7", "CA"))
      badProvince.hasErrors must beTrue

      val goodProvince = friendAddressForm.bind(address("80 Elgin St", "", "Ottawa", "Ontario", "K1P 5K7", "CA"))
      goodProvince.hasErrors must beFalse
    }

    "require a valid state for paid members when country is US" in {
      val badState = paidAddressForm.bind(address("Kings Place", "90 York Way", "London", "Greater London", "N1 9GU", "US"))
      badState.hasErrors must beTrue

      val goodState = paidAddressForm.bind(address("The Guardian", "536 Broadway", "New York", "New York", "10012", "US"))
      goodState.hasErrors must beFalse
    }

    "require a valid province for paid members when country is Canada" in {
      val badProvince = paidAddressForm.bind(address("80 Elgin St", "", "Ottawa", "Oldfoundland", "K1P 5K7", "CA"))
      badProvince.hasErrors must beTrue

      val goodProvince = paidAddressForm.bind(address("80 Elgin St", "", "Ottawa", "Ontario", "K1P 5K7", "CA"))
      goodProvince.hasErrors must beFalse
    }

    "require a valid country" in {
      val badCountryPaid = paidAddressForm.bind(address("No where", "", "City", "State", "Postcode", "Not a country"))
      badCountryPaid.hasErrors must beTrue

      val badCountryFriend = friendAddressForm.bind(address("No where", "", "City", "State", "Postcode", "Not a country"))
      badCountryFriend.hasErrors must beTrue
    }

    "concatenate lineOne and lineTwo" in {
      AddressForm("one", "two", "town", "county", "postcode", Countries.UK).line mustEqual "one, two"
      AddressForm("one", "", "town", "county", "postcode", Countries.UK).line mustEqual "one"
      AddressForm("", "two", "town", "county", "postcode", Countries.UK).line mustEqual "two"
    }
  }
}
