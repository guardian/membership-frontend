package views

import com.github.nscala_time.time.Imports._
import org.joda.time.{ PeriodType, Hours, Instant }

object Dates {

  implicit class RichInstant(dateTime: Instant) {
    val formatter = DateTimeFormat.mediumDateTime()

    lazy val pretty = formatter.print(dateTime)
  }

  implicit class RichDateTime(dateTime: DateTime) {
    val formatter = DateTimeFormat.forPattern("MMMM d, y k:m a")

    lazy val pretty = formatter.print(dateTime).replace("AM", "am").replace("PM", "pm")
  }

  def fromTimestamp(timestamp: Long): DateTime = new DateTime(timestamp * 1000)

  def todayDay = addSuffix(DateTime.now.toString("dd").toInt)

  def addSuffix(day: Int): String = {

    val suffix = (day % 10) match {
      case 1 => "st"
      case 2 => "nd"
      case 3 => "rd"
      case _ => "th"
    }

    day + suffix
  }
}
