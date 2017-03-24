package views.support

import org.joda.time.DateTime
import org.joda.time.Interval
import org.specs2.mutable.Specification

class PricingTest extends Specification {


  "bigDecimalToPrice" should {
    "show 1 as 1 and not 1.00" in {
      val bigDecimal = BigDecimal("1")
      Pricing.bigDecimalToPrice(bigDecimal) mustEqual  "1"
    }

    "show 1.5 as 1.50" in {
      val bigDecimal = BigDecimal("1.5")
      Pricing.bigDecimalToPrice(bigDecimal) mustEqual  "1.50"
    }

    "show 0.123 as 0.12" in {
      val bigDecimal = BigDecimal("0.123")
      Pricing.bigDecimalToPrice(bigDecimal) mustEqual  "0.12"
    }

    "show 0.127 as 0.13" in {
      val bigDecimal = BigDecimal("0.127")
      Pricing.bigDecimalToPrice(bigDecimal) mustEqual  "0.13"
    }

    "show 0.125 as 0.13" in {
      val bigDecimal = BigDecimal("0.125")
      Pricing.bigDecimalToPrice(bigDecimal) mustEqual  "0.13"
    }
  }
}
