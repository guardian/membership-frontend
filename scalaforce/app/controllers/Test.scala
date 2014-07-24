package controllers

import scala.concurrent.ExecutionContext.Implicits.global

import com.typesafe.config.ConfigFactory

import play.api.mvc.{Action, Controller}
import play.api.data._
import play.api.data.Forms._
import play.api.libs.json.{JsValue, Json}
import play.api.libs.ws.WS
import play.api.Play.current
import play.api.Logger
import com.gu.membership.salesforce.Scalaforce

object Test extends Controller {
  def start = Action {
    Ok(views.html.test())
  }

  val form = Form { tuple("url" -> text, "method" -> text, "body" -> text) }

  def run = Action.async { implicit request =>
    val (url, method, body) = form.bindFromRequest.get

    for {
      authentication <- Testforce.getAuthentication
      result <- WS.url(authentication.instance_url + url)
        .withHeaders("Authorization" -> s"Bearer ${authentication.access_token}")
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

  def login(endpoint: String, params: Seq[(String, String)]) = {
    val futureResult = WS.url(apiURL + endpoint).withQueryString(params: _*).post("")

    futureResult.foreach { result =>
      Logger.debug(s"LOGIN result ${result.status}")
      Logger.debug(result.body)
    }
    futureResult
  }

  def get(endpoint: String) = {
    val futureResult = WS.url(apiURL + endpoint).withHeaders("Authoriation" -> s"Bearer token").get()

    futureResult.foreach { result =>
      Logger.debug(s"GET result ${result.status}")
      Logger.debug(result.body)
    }
    futureResult
  }

  def patch(endpoint: String, body: JsValue) = {
    val futureResult = WS.url(apiURL + endpoint).withHeaders("Authorization" -> s"Bearer token").patch(body)

    futureResult.foreach { result =>
      Logger.debug(s"PATCH result ${result.status}")
      Logger.debug(result.body)
    }
    futureResult
  }

}
