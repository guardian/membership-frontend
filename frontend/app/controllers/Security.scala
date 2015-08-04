package controllers

import com.typesafe.scalalogging.LazyLogging
import monitoring.SentryLogging
import net.kencochrane.raven.event.{Event => RavenEvent, EventBuilder}
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.mvc.Controller

import scala.concurrent.Future

object Security extends Controller with LazyLogging {

  /**
   * See: https://developer.mozilla.org/en-US/docs/Web/Security/CSP/Using_CSP_violation_reports
   * Report format:
   *
   *  {
   *    "csp-report": {
   *      "document-uri": "http://example.com/signup.html",
   *      "referrer": "",
   *      "blocked-uri": "http://example.com/css/style.css",
   *      "violated-directive": "style-src cdn.example.com",
   *      "original-policy": "default-src 'none'; style-src cdn.example.com; report-uri /_/csp-reports"
   *    }
   *  }
   */
  case class CSPReport(
    documentUri: String,
    blockedUri: String,
    violatedDirective: String,
    referrer: Option[String]
  ) {
    val message = s"""
      |CSP warning:
      |Document URI: $documentUri
      |Blocked URI: $blockedUri
      |Violated Directive: $violatedDirective
      |Referrer: $referrer
      |""".stripMargin
  }

  implicit val reads: Reads[CSPReport] = (
    (JsPath \ "csp-report" \ "document-uri").read[String] and
    (JsPath \ "csp-report" \ "blocked-uri").read[String] and
    (JsPath \ "csp-report" \ "violated-directive").read[String] and
    (JsPath \ "csp-report" \ "referrer").readNullable[String]
  )(CSPReport.apply _)

  def cspReport = NoCacheAction.async(parse.tolerantJson(maxLength = 4096)) { implicit request =>

    val report = request.body.as[CSPReport]

    SentryLogging.ravenOpt.fold {
      logger.error(report.message)
    } {
      _.sendEvent(new EventBuilder()
        .withMessage(report.message)
        .withLevel(RavenEvent.Level.INFO).build())
    }

    Future.successful(Ok)
  }

}
