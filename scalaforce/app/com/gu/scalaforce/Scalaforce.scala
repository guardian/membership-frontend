package com.gu.scalaforce

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.Play.current
import play.api.libs.ws.WS

trait Scalaforce {
  val consumerKey: String
  val consumerSecret: String

  val apiURL: String
  val apiUsername: String
  val apiPassword: String
  val apiToken: String

  def getAccessToken: Future[String] = {
    WS.url(apiURL + "/services/oauth2/token")
      .withQueryString(
        "client_id" -> consumerKey,
        "client_secret" -> consumerSecret,
        "username" -> apiUsername,
        "password" -> (apiPassword + apiToken),
        "grant_type" -> "password"
      ).post("").map { result =>
        (result.json \ "access_token").as[String]
      }
  }

  object Contact {
  }
}

