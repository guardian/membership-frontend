package com.gu.membership.salesforce

import play.api.Logger
import play.api.Play.current
import play.api.libs.ws.WS
import play.api.libs.json.{Json, JsValue}
import play.api.libs.concurrent.Execution.Implicits._

trait ScalaforceWS extends Scalaforce {
  def authentication: Authentication

  private def requestWithAuth(endpoint: String) = {
    WS.url(authentication.instance_url + endpoint)
      .withHeaders("Authorization" -> s"Bearer ${authentication.access_token}")
  }

  def login(endpoint: String, params: Seq[(String, String)]) =
    WS.url(apiURL + endpoint).withQueryString(params: _*).post("")

  def get(endpoint: String) = {
    Logger.debug(s"MemberService: get $endpoint")

    val futureResult = requestWithAuth(endpoint).get()
    futureResult.foreach { result =>
      Logger.debug(s"MemberService: get response ${result.status}")
      Logger.debug(result.body)
    }
    futureResult
  }

  def patch(endpoint: String, body: JsValue) = {
    Logger.debug(s"MemberService: patch $endpoint")
    Logger.debug(Json.prettyPrint(body))

    val futureResult = requestWithAuth(endpoint).patch(body)
    futureResult.foreach { result =>
      Logger.debug(s"MemberService: patch response ${result.status}")
      Logger.debug(result.body)
    }
    futureResult
  }
}
