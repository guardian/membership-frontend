package controllers

import org.joda.time.DateTime
import play.api.mvc.SimpleResult
import com.github.nscala_time.time.Imports._

object Cached {

  private val cacheableStatusCodes = Seq(200, 404)

  def apply(result: SimpleResult): SimpleResult = apply(60)(result)

  def apply(seconds: Int)(result: SimpleResult): SimpleResult = {
    if (suitableForCaching(result)) cacheHeaders(seconds, result) else result
  }

  def suitableForCaching(result: SimpleResult): Boolean = cacheableStatusCodes.exists(_ == result.header.status)

  private def cacheHeaders(maxAge: Int, result: SimpleResult) = {
    val now = DateTime.now
    result.withHeaders(
      "Cache-Control" -> s"max-age=$maxAge",
      "Expires" -> toHttpDateTimeString(now + maxAge.seconds),
      "Date" -> toHttpDateTimeString(now)
    )
  }

  //http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1
  private val HTTPDateFormat = DateTimeFormat.forPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'").withZone(DateTimeZone.UTC)
  def toHttpDateTimeString(dateTime: DateTime): String = dateTime.toString(HTTPDateFormat)
}

object NoCache {
  def apply(result: SimpleResult): SimpleResult = result.withHeaders("Cache-Control" -> "no-cache", "Pragma" -> "no-cache")
}

