package controllers

import play.api.mvc.Controller

trait Offers extends Controller {
  // TODO move this to CachedAction once this work is ready to go into the wild
  def subscriber = GoogleAuthenticatedStaffAction { implicit request =>
    Ok(views.html.offer.subscriber())
  }
}

object Offers extends Offers
