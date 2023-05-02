package com.gu.i18n

import com.gu.memsub.Address
import org.specs2.mutable.Specification

class AddressTest extends Specification {
  val IE = CountryGroup.countryByCode("IE")

  "Address" should {
    "require only a postcode" in {
      Address("lineOne", "lineTwo", "town", "county", "", "GB").valid must beFalse

      Address("", "", "", "", "postCode", "GB").valid must beTrue
    }

    "require only lineOne and town in Ireland" in {
      Address("", "", "", "", "", "IE").valid must beFalse
      Address("lineOne", "", "", "", "", "IE").valid must beFalse
      Address("", "", "town", "", "", "IE").valid must beFalse

      Address("lineOne", "", "town", "", "", "IE").valid must beTrue
    }

    "require only postcode and valid state in the US" in {
      Address("", "", "", "", "", "US").valid must beFalse
      Address("", "", "", "", "postCode", "US").valid must beFalse
      Address("", "", "", "New York", "", "US").valid must beFalse
      Address("", "", "", "Greater York", "postCode", "US").valid must beFalse

      Address("", "", "", "New York", "postCode", "US").valid must beTrue
    }

    "require only postcode and valid province in CA" in {
      Address("", "", "", "", "", "CA").valid must beFalse
      Address("", "", "", "", "postCode", "CA").valid must beFalse
      Address("", "", "", "Quebec", "", "CA").valid must beFalse
      Address("", "", "", "Old Quebec", "postCode", "CA").valid must beFalse

      Address("", "", "", "Quebec", "postCode", "CA").valid must beTrue
    }

    "concatenate lineOne and lineTwo" in {
      Address("one", "two", "town", "county", "postCode", "GB").line mustEqual "one, two"
      Address("one", "", "town", "county", "postCode", "GB").line mustEqual "one"
      Address("", "two", "town", "county", "postCode", "GB").line mustEqual "two"
    }
  }
}
