package controllers

import play.api.mvc.Controller

trait Offer extends Controller {
  def subscriber = NoCacheAction { implicit request =>
    Ok(views.html.offer.subscriber())
  }
}

object Offer extends Offer
