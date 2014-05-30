package controllers

import play.api.mvc.{ Controller, Action }
import play.api.libs.json.Json

import services.{ AwsMemberTable, AuthenticationService }
import model.Tier

trait User extends Controller {
  def me = Action { implicit request =>
    val tier = AuthenticationService.authenticatedRequestFor(request).map { authRequest =>
      AwsMemberTable.get(authRequest.user.id).fold(Tier.RegisteredUser)(_.tier)
    }.getOrElse(Tier.AnonymousUser)

    Ok(Json.obj("tier" -> tier.toString))
  }
}

object User extends User

