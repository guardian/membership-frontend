package controllers

import com.typesafe.scalalogging.LazyLogging
import monitoring.SentryLogging
import play.api.libs.json.Json
import play.api.mvc.Controller

import scala.concurrent.Future

object Security extends Controller with LazyLogging {

  def cspReport = NoCacheAction.async(parse.tolerantJson(maxLength = 4096)) { implicit request =>

    val message = s"CSP warning ${Json.prettyPrint(request.body)}"

    SentryLogging.ravenOpt.fold {
      logger.error(message)
    } {
      _.sendMessage(message)
    }

    Future.successful(Ok)
  }

}
