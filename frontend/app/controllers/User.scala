package controllers

import play.api.mvc.{ Controller, Action }
import play.api.libs.json.Json
import configuration.Config
import services.{ AwsMemberTable, AuthenticationService }
import model.Tier

trait User extends Controller {
  def me = NoCacheAction { implicit request =>
    val authRequest = AuthenticationService.authenticatedRequestFor(request)
    val tier = authRequest.fold(Tier.AnonymousUser) { AwsMemberTable getTier _.user.id }

    Ok(Json.obj("userId" -> authRequest.map(_.user.id), "tier" -> tier.toString)).withHeaders(
      ("Access-Control-Allow-Origin", Config.corsAllowOrigin),
      ("Access-Control-Allow-Methods", "GET"),
      ("Access-Control-Allow-Credentials", "true")
    )

  }
}

object User extends User

