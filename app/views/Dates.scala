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

    //    val formatter = new PeriodFormatterBuilder()
    //      .printZeroAlways()
    //      .appendDays()
    //      .appendSeparator("d ")
    //      .appendHours()
    //      .appendSeparator("h ")
    //      .appendMinutes()
    //      .appendLiteral("m")
    //      .toFormatter()

    val formatter = DateTimeFormat.mediumDateTime()

    lazy val pretty = formatter.print(dateTime)
  }

  implicit class RichDateTime(dateTime: DateTime) {
    val DateFormat = "MMMM d, y k:m a"
    val formatter = DateTimeFormat.forPattern(DateFormat)
    lazy val pretty = formatter.print(dateTime).replace("AM", "am").replace("PM", "pm")
  }
}
