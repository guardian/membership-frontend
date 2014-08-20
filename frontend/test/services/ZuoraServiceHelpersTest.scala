package services

import org.specs2.mutable.Specification
import org.joda.time.DateTime

class ZuoraServiceHelpersTest extends Specification {
  "ZuoraServiceHelpers" should {
    "never use Z for UTC offset" in {
      val d = new DateTime("2012-01-01T10:00:00Z")
      ZuoraServiceHelpers.formatDateTime(d) mustEqual "2012-01-01T10:00:00.000+00:00"
    }

    "convert any timezone to UTC" in {
      val d = new DateTime("2012-01-01T10:00:00+08:00")
      ZuoraServiceHelpers.formatDateTime(d) mustEqual "2012-01-01T02:00:00.000+00:00"
    }
  }
}
