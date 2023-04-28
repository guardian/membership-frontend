package com.gu.zuora.soap

import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{LocalDate, DateTime, DateTimeZone}

object DateTimeHelpers {
  def formatDateTime(dt: DateTime): String = {
    val str = ISODateTimeFormat.dateTime().print(dt.withZone(DateTimeZone.UTC))
    str.replace("Z", "+00:00")
  }
}