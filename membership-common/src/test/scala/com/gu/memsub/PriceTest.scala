package com.gu.memsub

import com.gu.i18n.Currency._
import org.specs2.mutable.Specification

class PriceTest extends Specification  {
  "A price" should {
    "be prettified" in {
      Price(4.99f, USD).pretty shouldEqual "US$4.99"
      Price(4.9949f, USD).pretty shouldEqual "US$4.99"
      Price(4.995f, USD).pretty shouldEqual "US$5"
      Price(5f, GBP).pretty shouldEqual "£5"
    }

    "support basic binary operations with other prices" in {
      Price(5f, GBP) + 5f shouldEqual Price(10f, GBP)
      Price(5f, GBP) - 5f shouldEqual Price(0f, GBP)
      Price(5f, GBP) * 5f shouldEqual Price(25f, GBP)
      Price(5f, GBP) / 5f shouldEqual Price(1f, GBP)

      Price(5f, GBP) + Price(5f, GBP) shouldEqual Price(10f, GBP)
      Price(5f, GBP) - Price(5f, GBP) shouldEqual Price(0f, GBP)
      Price(5f, GBP) * Price(5f, GBP) shouldEqual Price(25f, GBP)
      Price(5f, GBP) / Price(5f, GBP) shouldEqual Price(1f, GBP)

      Price(5f, GBP) + Price(5f, USD) should throwAn[AssertionError]
    }
  }
}
