package services.zuora

import org.joda.time.DateTime
import org.specs2.mutable.Specification

import model.Zuora.ZuoraQuery
import model.ZuoraReaders.ZuoraQueryReader

class ZuoraServiceHelpersTest extends Specification {
  "formatDateTime" should {
    "never use Z for UTC offset" in {
      val d = new DateTime("2012-01-01T10:00:00Z")
      ZuoraServiceHelpers.formatDateTime(d) mustEqual "2012-01-01T10:00:00.000+00:00"
    }

    "convert any timezone to UTC" in {
      val d = new DateTime("2012-01-01T10:00:00+08:00")
      ZuoraServiceHelpers.formatDateTime(d) mustEqual "2012-01-01T02:00:00.000+00:00"
    }
  }

  "formatQuery" should {
    case class ZuoraQueryTest() extends ZuoraQuery

    "format a query with one field" in {
      val reader = ZuoraQueryReader("TestTable", Seq("Field1")) { result =>
        ZuoraQueryTest()
      }

      val q = ZuoraServiceHelpers.formatQuery(reader, "Field1='something'")
      q mustEqual "SELECT Field1 FROM TestTable WHERE Field1='something'"
    }

    "format a query with multiple fields" in {
      val reader = ZuoraQueryReader("TestTable", Seq("Field1", "Field2", "Field3")) { result =>
        ZuoraQueryTest()
      }

      val q = ZuoraServiceHelpers.formatQuery(reader, "Field2='blah'")
      q mustEqual "SELECT Field1,Field2,Field3 FROM TestTable WHERE Field2='blah'"
    }
  }
}
