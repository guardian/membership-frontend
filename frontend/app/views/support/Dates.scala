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

  def prettyDate(dt: DateTime): String = dt.toString("dd MMMMM YYYY")

  def prettyDateWithTime(dt: DateTime): String =
    prettyDate(dt) + dt.toString(", h:mm ") + dt.toString("a").toLowerCase

  def dayInMonthWithSuffix(date: DateTime = DateTime.now): Html = addSuffix(date.toString("dd").toInt)

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
}
