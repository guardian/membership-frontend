package filters

import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.mvc._

import scala.concurrent.Future

object AddCSPHeader extends Filter {

  private val scriptWhitelist = Seq(
    "js.stripe.com",
    "*.ophan.co.uk",
    "*.twitter.com",
    "*.ytimg.com",
    "*.youtube.com",
    "*.google-analytics.com",
    "*.krxd.net",
    "secure.adnxs.com",
    "script.crazyegg.com"
  ).mkString(" ")

  private val frameWhitelist = Seq(
    "js.stripe.com",
    "*.youtube.com"
  ).mkString(" ")

  private val imageWhitelist = Seq(
    "*.guim.co.uk",
    "*.theguardian.com",
    "*.krxd.net",
    "*.google-analytics.com",
    "*.ytimg.com",
    "t.co",
    "*.twitter.com",
    "sb.scorecardresearch.com"
  ).mkString(" ")

  private val fontWhitelist = Seq(
    "pasteup.guim.co.uk"
  ).mkString(" ")

  val cspHeaders = "Content-Security-Policy-Report-Only" -> Seq(
    "default-src 'self'",
    "style-src 'self' 'unsafe-inline'",
    "script-src 'self' 'unsafe-inline' 'unsafe-eval' " + scriptWhitelist,
    "frame-src 'self' " + frameWhitelist,
    "img-src 'self' data: " + imageWhitelist,
    "font-src 'self' " + fontWhitelist,
    "report-uri /csp-report"
  ).mkString("; ")

  def apply(nextFilter: RequestHeader => Future[Result])(requestHeader: RequestHeader): Future[Result] = for {
    result <- nextFilter(requestHeader)
  } yield {
      val isHtml = result.header.headers.get("Content-Type").exists(_.contains("text/html"))
      if (isHtml) result.withHeaders(cspHeaders) else result
    }
}
