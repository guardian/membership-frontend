package com.gu.i18n

import org.specs2.mutable.Specification
import Currency._

class CountryGroupTest extends Specification {
  "CountryGroupTest" should {

    "byId" in {
      CountryGroup.byId("ie") should_=== None
      CountryGroup.byId("eu") should_=== Some(CountryGroup.Europe)
      CountryGroup.byId("int") should_=== Some(CountryGroup.RestOfTheWorld)
    }

    "byCountryNameOrCode" in {
      CountryGroup.byCountryNameOrCode(Country.Australia.alpha2) should_=== Some(CountryGroup.Australia)
      CountryGroup.byCountryNameOrCode(Country.Australia.name) should_=== Some(CountryGroup.Australia)
      CountryGroup.byCountryNameOrCode(Country.US.alpha2) should_=== Some(CountryGroup.US)
      CountryGroup.byCountryNameOrCode(Country.US.name) should_=== Some(CountryGroup.US)
      CountryGroup.byCountryNameOrCode("Italy") should_=== Some(CountryGroup.Europe)
      CountryGroup.byCountryNameOrCode("IT") should_=== Some(CountryGroup.Europe)
      CountryGroup.byCountryNameOrCode("AF") should_=== Some(CountryGroup.RestOfTheWorld)
      CountryGroup.byCountryNameOrCode("Afghanistan") should_=== Some(CountryGroup.RestOfTheWorld)
      CountryGroup.byCountryNameOrCode("IE") should_=== Some(CountryGroup.Europe)
    }

    "availableCurrency" in {
      CountryGroup.availableCurrency(Set.empty)(Country.UK) should_=== None
      CountryGroup.availableCurrency(Set(GBP, AUD))(Country.US) should_=== None
      CountryGroup.availableCurrency(Set(GBP, AUD))(Country.UK) should_=== Some(GBP)
      CountryGroup.availableCurrency(Set(GBP, AUD))(Country.Australia) should_=== Some(AUD)
    }
  }
}
