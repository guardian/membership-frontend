package views.support

import com.github.nscala_time.time.Imports._
import org.joda.time.Instant
import play.twirl.api.Html

object Dates {

  def prettyDate(dt: DateTime): String = dt.toString("dd MMMMM YYYY")
  def prettyDate(dt: Long): String = prettyDate(new DateTime(dt * 1000))

  def prettyDateWithTime(dt: DateTime): String =
    prettyDate(dt) + dt.toString(", HH:mm a").replace("AM", "am").replace("PM", "pm")

  def prettyDateWithTime(dt: Instant): String = prettyDateWithTime(new DateTime(dt))

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
