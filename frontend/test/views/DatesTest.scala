package views

import org.specs2.mutable.Specification

class DatesTest extends Specification {
  "addSuffix" should {

    "append the correct suffix for 'th'" in {
      Dates.suffix(4) mustEqual "th"
      Dates.suffix(5) mustEqual "th"
      Dates.suffix(6) mustEqual "th"
      Dates.suffix(7) mustEqual "th"
      Dates.suffix(8) mustEqual "th"
      Dates.suffix(9) mustEqual "th"
      Dates.suffix(10) mustEqual "th"
      Dates.suffix(11) mustEqual "th"
      Dates.suffix(12) mustEqual "th"
      Dates.suffix(13) mustEqual "th"
      Dates.suffix(14) mustEqual "th"
      Dates.suffix(15) mustEqual "th"
      Dates.suffix(16) mustEqual "th"
      Dates.suffix(17) mustEqual "th"
      Dates.suffix(18) mustEqual "th"
      Dates.suffix(19) mustEqual "th"
      Dates.suffix(20) mustEqual "th"
      Dates.suffix(24) mustEqual "th"
      Dates.suffix(25) mustEqual "th"
      Dates.suffix(26) mustEqual "th"
      Dates.suffix(27) mustEqual "th"
      Dates.suffix(28) mustEqual "th"
      Dates.suffix(29) mustEqual "th"
      Dates.suffix(30) mustEqual "th"
    }

    "append the correct suffix for 'st" in {
      Dates.suffix(1) mustEqual "st"
      Dates.suffix(21) mustEqual "st"
      Dates.suffix(31) mustEqual "st"
    }

    "append the correct suffix for 'nd" in {
      Dates.suffix(2) mustEqual "nd"
      Dates.suffix(22) mustEqual "nd"
    }

    "append the correct suffix for 'rd" in {
      Dates.suffix(3) mustEqual "rd"
      Dates.suffix(23) mustEqual "rd"
    }
  }
}
