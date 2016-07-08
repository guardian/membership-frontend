package controllers

import org.specs2.mutable.Specification

class CreditCardExpiryYears$Test extends Specification {

  "CreditCardExpiryYears$Test" should {
    "display next 10 years prefix from 2016" in {
      CreditCardExpiryYears(2016, 10) must_== (16 to 25).toList
    }
    "display next 10 years prefix from 2017" in {
      CreditCardExpiryYears(2017, 10) must_== (17 to 26).toList
    }
  }
}
