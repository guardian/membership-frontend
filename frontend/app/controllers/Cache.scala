package controllers

import org.joda.time.DateTime
import play.api.mvc.SimpleResult
import views.Dates._
import com.github.nscala_time.time.Imports._

object Cached {

  private val cacheableStatusCodes = Seq(200, 404)

  def apply(result: SimpleResult): SimpleResult = apply(60)(result)

  def apply(seconds: Int)(result: SimpleResult): SimpleResult = {
    if (cacheableStatusCodes.exists(_ == result.header.status)) cacheHeaders(seconds, result) else result
  }

  private def cacheHeaders(maxAge: Int, result: SimpleResult) = {
    val now = DateTime.now
    result.withHeaders(
      "Cache-Control" -> s"max-age=$maxAge",
      "Expires" -> (now + maxAge.seconds).toHttpDateTimeString,
      "Date" -> now.toHttpDateTimeString
    )
  }
}

object NoCache {
  def apply(result: SimpleResult): SimpleResult = result.withHeaders("Cache-Control" -> "no-cache", "Pragma" -> "no-cache")
}

