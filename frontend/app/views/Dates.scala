package views

import com.github.nscala_time.time.Imports._
import org.joda.time.{ PeriodType, Hours, Instant }
import java.io.Serializable
import org.joda.convert.FromString
import org.joda.time.base.BasePeriod
import org.joda.time.chrono.ISOChronology
import org.joda.time.field.FieldUtils
import org.joda.time.format.{ PeriodFormatterBuilder, ISOPeriodFormat, PeriodFormatter }

object Dates {

  implicit class RichInstant(dateTime: Instant) {
    val formatter = DateTimeFormat.mediumDateTime()

    lazy val pretty = formatter.print(dateTime)
  }

  implicit class RichDateTime(dateTime: DateTime) {
    val formatter = DateTimeFormat.forPattern("MMMM d, y k:m a")

    lazy val pretty = formatter.print(dateTime).replace("AM", "am").replace("PM", "pm")
  }

  def todayDate() = DateTimeFormat.forPattern("dd/MM/yyyy").print(new DateTime())
}
