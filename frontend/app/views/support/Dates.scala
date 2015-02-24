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
    def pretty(includeTime: Boolean): String = if (includeTime) prettyWithTime else pretty
  }

  def prettyDate(dt: DateTime): String = dt.toString("d MMMMM YYYY")
  def prettyTime(dt: DateTime): String = dt.toString("h:mm") + dt.toString("a").toLowerCase

  def prettyDateWithTime(dt: DateTime): String = prettyDate(dt) + ", " + prettyTime(dt)

  def prettyDateWithTimeAndDayName(dt: DateTime): String =
    dt.toString("EEEE ") + prettyDateWithTime(dt)

  def prettyDateAndDayName(dt: DateTime): String =
    dt.toString("EE ") + prettyDate(dt)

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

  case class Range(start: String, end: String)

  def dateRange(start: DateTime, end: DateTime): Range = {
    def dateToks(dt: DateTime) = dt.toString("EEEE d MMMMM YYYY").split(" ").reverse

    if (start.withTimeAtStartOfDay() == end.withTimeAtStartOfDay()) {
      Range(prettyDateWithTimeAndDayName(start), prettyTime(end))
    } else {
      val startToks = dateToks(start)
      val endToks = dateToks(end)

      val newStartToks = (startToks zip endToks).dropWhile { case (a, b) => a == b }.map { case (a, b) => a }

      Range(newStartToks.reverse.mkString(" "), endToks.reverse.mkString(" "))
    }
  }
}
