package controllers

import com.github.nscala_time.time.Imports._
import org.joda.time.DateTime
import play.api.mvc.Result

import scala.math.max

object Cached {

  private val cacheableStatusCodes = Seq(200, 301, 404)

  private val tenDaysInSeconds = 10.days.standardDuration.seconds

  def apply(result: Result): Result = apply(60)(result)

  def apply(seconds: Int)(result: Result): Result = {
    if (suitableForCaching(result)) cacheHeaders(seconds, result) else result
  }

  def suitableForCaching(result: Result): Boolean =
    cacheableStatusCodes.exists(_ == result.header.status)

  private def cacheHeaders(maxAge: Int, result: Result) = {
    val now = DateTime.now
    val staleWhileRevalidateSeconds = max(maxAge / 10, 1)
    result.withHeaders(
        "Cache-Control" -> s"public, max-age=$maxAge, stale-while-revalidate=$staleWhileRevalidateSeconds, stale-if-error=$tenDaysInSeconds",
        "Expires" -> toHttpDateTimeString(now + maxAge.seconds),
        "Date" -> toHttpDateTimeString(now)
    )
  }

  //http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html#sec3.3.1
  private val HTTPDateFormat = DateTimeFormat
    .forPattern("EEE, dd MMM yyyy HH:mm:ss 'GMT'")
    .withZone(DateTimeZone.UTC)
  def toHttpDateTimeString(dateTime: DateTime): String =
    dateTime.toString(HTTPDateFormat)
}

object NoCache {
  def apply(result: Result): Result =
    result.withHeaders(
        "Cache-Control" -> "no-cache, private", "Pragma" -> "no-cache")
}
