package views

import com.github.nscala_time.time.Imports._
import org.joda.time.Instant

object Dates {

  implicit class RichInstant(dateTime: Instant) {
    val date = new DateTime(dateTime)
    lazy val pretty = dayWithSuffix(date) + date.toString(" MMMMM YYYY, h:mma").replace("AM", "am").replace("PM", "pm")
  }

  implicit class RichDateTime(dateTime: DateTime) {
    val date = new DateTime(dateTime)
    lazy val pretty = dayWithSuffix(date) + date.toString(" MMMMM YYYY, h:mma").replace("AM", "am").replace("PM", "pm")
  }

  implicit class RichLong(dateTime: Long) {
    val date = new DateTime(dateTime * 1000)
    lazy val pretty = dayWithSuffix(date) + date.toString(" MMMMM YYYY, h:mma").replace("AM", "am").replace("PM", "pm")
  }

  def dayWithSuffix(date: DateTime): String = addSuffix(date.toString("dd").toInt)

  def dayInMonthWithSuffix(date: DateTime = DateTime.now): String = dayWithSuffix(date)

  def addSuffix(day: Int): String = {
    var suffix = (day % 10) match {
      case 1 => "st"
      case 2 => "nd"
      case 3 => "rd"
      case _ => "th"
    }
    if (day == 12){
      suffix = "th"
    }
    day + "<sup>" + suffix + "</sup>"
  }
}
