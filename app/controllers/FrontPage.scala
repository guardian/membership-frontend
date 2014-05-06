package controllers

import play.api.mvc.{Action, Controller}
import services.{EventbriteService, EventService}
import scala.concurrent.ExecutionContext.Implicits.global

trait FrontPage extends Controller{

  def index = Action {
    Ok(views.html.index())
  }

}
object FrontPage extends FrontPage
