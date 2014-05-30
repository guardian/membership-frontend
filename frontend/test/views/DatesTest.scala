package views

import org.specs2.mutable.Specification
import org.joda.time.DateTime

class DatesTest extends Specification {

  "Dates" should {
    "Convert to http date string" in {
      import Dates._
      val theDate = new DateTime(2001, 5, 20, 12, 3, 4, 555)
      theDate.toHttpDateTimeString mustEqual "Sun, 20 May 2001 11:03:04 GMT"
    }
  }

}
