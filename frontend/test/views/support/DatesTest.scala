package views.support

import org.joda.time.DateTime
import org.joda.time.Interval
import org.specs2.mutable.Specification

class DatesTest extends Specification {

  "prettyTime" should {
    "respect Guardian style: 1am, 6.30pm, etc" in {
      Dates.prettyTime(new DateTime(2015, 3, 22,  1,  0)) mustEqual "1am"
      Dates.prettyTime(new DateTime(2015, 3, 22, 18, 30)) mustEqual "6.30pm"
    }
  }

  "dateRange" should {
    "respect Guardian style on date ranges" in {
      val interval = new Interval(new DateTime(2015, 3, 22, 14, 45),  new DateTime(2015, 3, 22, 17,  0))
      Dates.dateTimeRange(interval) mustEqual "Sunday 22 March 2015, 2.45pm–5pm"
    }

    "merge date if both dates are on the same day" in {
      val interval = new Interval(new DateTime(2014, 11, 6, 10, 20),  new DateTime(2014, 11, 6, 12, 30))
      Dates.dateTimeRange(interval) mustEqual "Thursday 6 November 2014, 10.20am–12.30pm"
    }

    "merge month and year if both are the same" in {
      val interval = new Interval(new DateTime(2014, 11, 6, 15, 0),  new DateTime(2014, 11, 8, 13, 0))
      Dates.dateTimeRange(interval) mustEqual "Thursday 6–Saturday 8 November 2014"
    }

    "merge year if both are the same" in {
      val interval = new Interval(new DateTime(2014, 11, 6, 15, 0),  new DateTime(2014, 12, 8, 13, 0))
      Dates.dateTimeRange(interval) mustEqual "Thursday 6 November–Monday 8 December 2014"
    }

    "show day, month and year for both if none match" in {
      val interval = new Interval(new DateTime(2014, 11, 6, 15, 0),  new DateTime(2015, 1, 6, 13, 0))
      Dates.dateTimeRange(interval) mustEqual "Thursday 6 November 2014–Tuesday 6 January 2015"
    }
  }

  "shortDateRangeString" should {
    "merge date if both dates are on the same day" in {
      val interval = new Interval(new DateTime(2014, 11, 6, 10, 20),  new DateTime(2014, 11, 6, 12, 30))
      Dates.dateRange(interval) mustEqual "6 November 2014"
    }
    "merge month and year if both are the same" in {
      val interval = new Interval(new DateTime(2014, 11, 6, 15, 0),  new DateTime(2014, 11, 8, 13, 0))
      Dates.dateRange(interval) mustEqual "6–8 November 2014"
    }
    "merge year if both are the same" in {
      val interval = new Interval(new DateTime(2014, 11, 6, 15, 0),  new DateTime(2014, 12, 8, 13, 0))
      Dates.dateRange(interval) mustEqual "6 November–8 December 2014"
    }
    "show day, month and year for both if none match" in {
      val interval = new Interval(new DateTime(2014, 11, 6, 15, 0),  new DateTime(2015, 1, 6, 13, 0))
      Dates.dateRange(interval) mustEqual "6 November 2014–6 January 2015"
    }
  }

  "addSuffix" should {

    "append the correct suffix for 'th'" in {
      forall((4 to 20) ++ (24 to 30)) ((num:Int) => Dates.suffix(num) mustEqual("th"))
    }

    "append the correct suffix for 'st" in {
      forall(Seq(1, 21, 31)) ((num:Int) => Dates.suffix(num) mustEqual("st"))
    }

    "append the correct suffix for 'nd" in {
      forall(Seq(2, 22)) ((num:Int) => Dates.suffix(num) mustEqual("nd"))
    }

    "append the correct suffix for 'rd" in {
      forall(Seq(3, 23)) ((num:Int) => Dates.suffix(num) mustEqual("rd"))
    }
  }
}
