package controllers

import play.api.mvc.Controller

trait Redirects extends Controller {

  def homepageRedirect = CachedAction(MovedPermanently("/"))

  def supporterRedirect = CachedAction {
    MovedPermanently(routes.Info.supporter.toString)
  }

}

object Redirects extends Redirects
