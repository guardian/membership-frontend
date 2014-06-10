package views

import org.specs2.mutable.Specification

class DatesTest extends Specification {
  "addSuffix" should {

    "append the correct suffix for 'th'" {
      Dates.suffix(5) mustEqual "th"
      Dates.suffix(11) mustEqual "th"
      Dates.suffix(12) mustEqual "th"
      Dates.suffix(13) mustEqual "th"
    }

    "append the correct suffix for 'st" {
      Dates.suffix(1) mustEqual "st"
    }

    "append the correct suffix for 'nd" {
      Dates.suffix(2) mustEqual "nd"
    }

    "append the correct suffix for 'rd" {
      Dates.suffix(3) mustEqual "rd"
    }
  }
}
