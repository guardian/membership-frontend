package controllers

import scala.concurrent.ExecutionContext.Implicits.global

import com.typesafe.config.ConfigFactory

import com.gu.scalaforce.Scalaforce

import play.api.mvc.{Action, Controller}
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.Json
import play.api.libs.ws.WS
import play.api.Play.current

object Test extends Controller {
  def start = Action {
    Ok(views.html.test())
  }

  val form = Form { tuple("url" -> text, "method" -> text, "body" -> text) }

  def run = Action.async { implicit request =>
    val (url, method, body) = form.bindFromRequest.get

    for {
      token <- Testforce.getAccessToken
      result <- WS.url(Testforce.apiURL + url)
        .withHeaders("Authorization" -> s"Bearer $token")
        .withBody(body)
        .execute(method)
    } yield {
      val resultBody = try {
        Json.prettyPrint(result.json)
      } catch {
        case _: Throwable => result.body
      }
      Ok(views.html.test(url, body, result.status, resultBody))
    }
  }
}

object Testforce extends Scalaforce {
  val config = ConfigFactory.load()

  val consumerKey = config.getString("salesforce.consumer.key")
  val consumerSecret = config.getString("salesforce.consumer.secret")

  val apiURL = config.getString("salesforce.api.url")
  val apiUsername = config.getString("salesforce.api.username")
  val apiPassword = config.getString("salesforce.api.password")
  val apiToken = config.getString("salesforce.api.token")
}
