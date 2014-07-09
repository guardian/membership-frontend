package com.gu.scalaforce

import scala.concurrent.{Await, Future}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import akka.agent.Agent

import play.api.Play.current
import play.api.libs.ws.WS
import play.api.libs.concurrent.Akka
import play.api.Logger

trait Scalaforce {
  val consumerKey: String
  val consumerSecret: String

  val apiURL: String
  val apiUsername: String
  val apiPassword: String
  val apiToken: String

  val accessToken = Agent[String]("")

  def request(endpoint: String, token: String) =
    WS.url(apiURL + endpoint).withHeaders("Authorization" -> s"Bearer $token")

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

  private implicit val system = Akka.system

  def refresh() {
    Logger.debug("Refreshing Scalaforce token")
    accessToken.sendOff(_ => {
      val token = Await.result(getAccessToken, 15.seconds)
      Logger.debug(s"Got token $token")
      token
    })
  }

  def start() {
    Logger.info("Starting Scalaforce background tasks")
    system.scheduler.schedule(5.seconds, 2.hours) { refresh() }
  }
}
