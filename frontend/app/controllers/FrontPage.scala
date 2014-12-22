package controllers

import play.api.mvc.Controller

trait FrontPage extends Controller {

  def index = CachedAction { implicit request =>
    Ok(views.html.index())
  }
}

object FrontPage extends FrontPage
