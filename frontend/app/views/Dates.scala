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

  def todayDate(format: String = "dd/MM/yyyy") = DateTime.now.toString(format)
}
