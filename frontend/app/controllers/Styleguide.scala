package controllers

import play.api.mvc.Controller

trait Styleguide extends Controller {

  def patterns = NoCacheAction { implicit request =>
    Ok(views.html.styleguide.patterns())
  }

}

object Styleguide extends Styleguide
