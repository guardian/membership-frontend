package controllers

import play.api.mvc.{ Controller, Action }
import play.api.libs.json.Json

import services.AuthenticationService

trait User extends Controller {
  def me = Action { implicit request =>
    val tier = AuthenticationService.authenticatedRequestFor(request)
      .fold("guest")(_ => "member")

    Ok(Json.obj("tier" -> tier))
  }
}

object User extends User
