package views.support

import com.github.nscala_time.time.Imports._
import org.joda.time.Instant
import play.twirl.api.Html

object Dates {

  implicit class RichDateTime(dt: DateTime) {
    lazy val pretty = prettyDate(dt)
    lazy val prettyWithTime = prettyDateWithTime(dt)
  }

  implicit class RichInstant(dt: Instant) {
    lazy val pretty = prettyDate(new DateTime(dt))
    lazy val prettyWithTime = prettyDateWithTime(new DateTime(dt))
  }

  def prettyDate(dt: DateTime): String = dt.toString("d MMMMM YYYY")
  def prettyTime(dt: DateTime): String = dt.toString("h:mm") + dt.toString("a").toLowerCase

  def prettyDateWithTime(dt: DateTime): String = prettyDate(dt) + ", " + prettyTime(dt)

  def prettyDateWithTimeAndDayName(dt: DateTime): String =
    dt.toString("EEEE ") + prettyDateWithTime(dt)

  def dayInMonthWithSuffix(date: DateTime = DateTime.now): Html = addSuffix(date.toString("dd").toInt)

  def dayInMonthWithSuffixAndMonth(date: DateTime = DateTime.now): Html = {
    Html(addSuffix(date.toString("dd").toInt) + " " + date.toString("MMMM"))
  }

  def addSuffix(day: Int): Html = Html(day + "<sup>" + suffix(day) + "</sup>")

  def suffix(day: Int) = day match {
    case 11 | 12 | 13 => "th"
    case _ => day % 10 match {
      case 1 => "st"
      case 2 => "nd"
      case 3 => "rd"
      case _ => "th"
    }
  }

  def dateRange(dt1: DateTime, dt2: DateTime): (String, String) = {
    def zipWithPadding(a: Seq[String], b: Seq[String]) = {
      val maxLen = a.length max b.length
      a.padTo(maxLen, "") zip b.padTo(maxLen, "")
    }

    if (dt1.withTimeAtStartOfDay() == dt2.withTimeAtStartOfDay()) {
      (prettyDateWithTimeAndDayName(dt1), prettyTime(dt2))
    } else {
      val dt1toks = dt1.toString("EEEE d MMMMM YYYY").split(" ").reverse
      val dt2toks = dt2.toString("EEEE d MMMMM YYYY").split(" ").reverse

      val start = zipWithPadding(dt1toks, dt2toks)
        .dropWhile { case (a, b) => a == b }
        .map { case (a, b) => a }

      (start.reverse.mkString(" "), dt2toks.reverse.mkString(" "))
    }
  }
}
