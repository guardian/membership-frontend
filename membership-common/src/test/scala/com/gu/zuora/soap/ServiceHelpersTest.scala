package com.gu.zuora.soap

import com.gu.zuora.soap.DateTimeHelpers.formatDateTime
import com.gu.zuora.soap.readers.Query
import org.joda.time.DateTime
import org.specs2.mutable.Specification

class DateTimeHelpersTest extends Specification {
  "formatDateTime" should {
    "never use Z for UTC offset" in {
      val d = new DateTime("2012-01-01T10:00:00Z")
      formatDateTime(d) mustEqual "2012-01-01T10:00:00.000+00:00"
    }

    "convert any timezone to UTC" in {
      val d = new DateTime("2012-01-01T10:00:00+08:00")
      formatDateTime(d) mustEqual "2012-01-01T02:00:00.000+00:00"
    }
  }

  "formatQuery" should {
    case class ZuoraQueryTest() extends models.Query

    "format a query with one field" in {
      val query = Query("TestTable", Seq("Field1")) { result =>
        ZuoraQueryTest()
      }

      val q = query.format("Field1='something'")
      q mustEqual "SELECT Field1 FROM TestTable WHERE Field1='something'"
    }

    "format a query with multiple fields" in {
      val query = Query("TestTable", Seq("Field1", "Field2", "Field3")) { result =>
        ZuoraQueryTest()
      }

      val q = query.format("Field2='blah'")
      q mustEqual "SELECT Field1,Field2,Field3 FROM TestTable WHERE Field2='blah'"
    }
  }
}
