package com.gu.scalaforce

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.libs.ws.WSResponse
import play.api.libs.json.JsValue

trait Scalaforce {
  val consumerKey: String
  val consumerSecret: String

  val apiURL: String
  val apiUsername: String
  val apiPassword: String
  val apiToken: String

  def login(endpoint: String, params: Seq[(String, String)]): Future[WSResponse]
  def get(endpoint: String): Future[WSResponse]
  def patch(endpoint: String, body: JsValue): Future[WSResponse]

  /**
   * This uses the Salesforce Username-Password Flow to get an access token.
   *
   *
   * https://help.salesforce.com/apex/HTViewHelpDoc?id=remoteaccess_oauth_username_password_flow.htm
   * https://www.salesforce.com/us/developer/docs/api_rest/Content/intro_understanding_username_password_oauth_flow.htm
   */
  def getAccessToken: Future[String] = {
    val params = Seq(
      "client_id" -> consumerKey,
      "client_secret" -> consumerSecret,
      "username" -> apiUsername,
      "password" -> (apiPassword + apiToken),
      "grant_type" -> "password"
    )

    login("/services/oauth2/token", params).map { result =>
      (result.json \ "access_token").as[String]
    }
  }
}
