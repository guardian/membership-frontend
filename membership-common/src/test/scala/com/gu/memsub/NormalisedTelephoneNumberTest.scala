package com.gu.memsub

import com.gu.i18n.Country
import org.specs2.mutable.Specification


class NormalisedTelephoneNumberTest extends Specification {
  "TelephoneNumberTest" should {
    "fromStringAndCountry" should {
      "return None when no number or country is provided" in {
        NormalisedTelephoneNumber.fromStringAndCountry(None, Some(Country.UK)) mustEqual None
        NormalisedTelephoneNumber.fromStringAndCountry(Some("02033532000"), None) mustEqual None
        NormalisedTelephoneNumber.fromStringAndCountry(None, None) mustEqual None
      }
      "return None when an invalid number is provided" in {
        NormalisedTelephoneNumber.fromStringAndCountry(Some(":)"), Some(Country.UK)) mustEqual None
        NormalisedTelephoneNumber.fromStringAndCountry(Some("000000000000000000000000"), Some(Country.UK)) mustEqual None
        NormalisedTelephoneNumber.fromStringAndCountry(Some("07777777"), Some(Country.UK)) mustEqual None
      }
      "process a UK local number (Kings Place) with spaces" in {
        NormalisedTelephoneNumber.fromStringAndCountry(Some("020 3353 2000"), Some(Country.UK)) mustEqual Some(NormalisedTelephoneNumber("44", "2033532000"))
      }
      "process a UK local number without spaces" in {
        NormalisedTelephoneNumber.fromStringAndCountry(Some("02033532000"), Some(Country.UK)) mustEqual Some(NormalisedTelephoneNumber("44", "2033532000"))
      }
      "process a UK local number without a leading 0" in {
        NormalisedTelephoneNumber.fromStringAndCountry(Some("2033532000"), Some(Country.UK)) mustEqual Some(NormalisedTelephoneNumber("44", "2033532000"))
      }
      "process a UK local number with a preceding 0044" in {
        NormalisedTelephoneNumber.fromStringAndCountry(Some("00442033532000"), Some(Country.UK)) mustEqual Some(NormalisedTelephoneNumber("44", "2033532000"))
      }
      "process a UK local number with a preceding +44" in {
        NormalisedTelephoneNumber.fromStringAndCountry(Some("+442033532000"), Some(Country.UK)) mustEqual Some(NormalisedTelephoneNumber("44", "2033532000"))
      }
      "process a UK local number with a preceding +44 from Ireland" in {
        NormalisedTelephoneNumber.fromStringAndCountry(Some("+442033532000"), Some(Country.Ireland)) mustEqual Some(NormalisedTelephoneNumber("44", "2033532000"))
      }
      "process a UK local number with a preceding +44 from the US" in {
        NormalisedTelephoneNumber.fromStringAndCountry(Some("+442033532000"), Some(Country.US)) mustEqual Some(NormalisedTelephoneNumber("44", "2033532000"))
      }
      "process a UK local number with a preceding +44 from Australia" in {
        NormalisedTelephoneNumber.fromStringAndCountry(Some("+442033532000"), Some(Country.Australia)) mustEqual Some(NormalisedTelephoneNumber("44", "2033532000"))
      }
      "process a US local number (NY office) with dashes" in {
        NormalisedTelephoneNumber.fromStringAndCountry(Some("212-231-7762"), Some(Country.US)) mustEqual Some(NormalisedTelephoneNumber("1", "2122317762"))
      }
      "process a US local number with brackets" in {
        NormalisedTelephoneNumber.fromStringAndCountry(Some("212(231)7762"), Some(Country.US)) mustEqual Some(NormalisedTelephoneNumber("1", "2122317762"))
      }
      "process a US local number with  a leading +1" in {
        NormalisedTelephoneNumber.fromStringAndCountry(Some("+12122317762"), Some(Country.US)) mustEqual Some(NormalisedTelephoneNumber("1", "2122317762"))
      }
      "process a US local number with  a leading +1 from Canada" in {
        NormalisedTelephoneNumber.fromStringAndCountry(Some("+12122317762"), Some(Country.Canada)) mustEqual Some(NormalisedTelephoneNumber("1", "2122317762"))
      }
      "process an AU local number (Sydney) with spaces" in {
        NormalisedTelephoneNumber.fromStringAndCountry(Some("02 8076 8500"), Some(Country.Australia)) mustEqual Some(NormalisedTelephoneNumber("61", "280768500"))
      }
      "process an AU local number (Sydney) with leading +61" in {
        NormalisedTelephoneNumber.fromStringAndCountry(Some("+612 8076 8500"), Some(Country.Australia)) mustEqual Some(NormalisedTelephoneNumber("61", "280768500"))
      }
    }
  }
}
