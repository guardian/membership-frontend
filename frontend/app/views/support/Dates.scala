package views.support

import com.github.nscala_time.time.Imports._
import org.joda.time.Instant
import play.twirl.api.Html

object Dates {

  implicit class RichInstant(dateTime: Instant) {
    val date = new DateTime(dateTime)
    lazy val pretty = date.toString("dd MMMMM YYYY, h:mma").replace("AM", "am").replace("PM", "pm")
  }

  implicit class RichDateTime(dateTime: DateTime) {
    val date = new DateTime(dateTime)
    lazy val pretty = date.toString("dd MMMMM YYYY, h:mma").replace("AM", "am").replace("PM", "pm")
  }

  implicit class RichLong(dateTime: Long) {
    val date = new DateTime(dateTime * 1000)
    lazy val pretty = date.toString("dd MMMMM YYYY, h:mma").replace("AM", "am").replace("PM", "pm")
  }

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
