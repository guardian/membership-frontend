package controllers

import play.api.mvc.{Action, Controller}

trait Info extends Controller {

  def help = CachedAction { implicit request =>
    Ok(views.html.info.help())
  }

  def feedback = CachedAction { implicit request =>
    Ok(views.html.info.feedback())
  }
}

object Info extends Info