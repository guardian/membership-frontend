package views

import com.github.nscala_time.time.Imports._
import org.joda.time.format.DateTimeFormatter

object Dates {

  implicit class RichDateTime(dateTime: DateTime) {


    val formatter = DateTimeFormat.mediumDateTime()

    lazy val pretty = formatter.print(dateTime)
  }
}
