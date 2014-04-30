package views

import com.github.nscala_time.time.Imports._

object Dates {

  implicit class RichDateTime(dateTime: DateTime) {
    val formatter = DateTimeFormat.mediumDateTime()
    lazy val pretty = formatter.print(dateTime)
  }
}
