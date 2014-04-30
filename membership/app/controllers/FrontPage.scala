package controllers

import play.api.mvc.{Action, Controller}

object FrontPage extends Controller{

  def index = Action {
    Ok(views.html.index())
  }

}