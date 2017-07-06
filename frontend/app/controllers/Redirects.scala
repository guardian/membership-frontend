package controllers

import play.api.mvc._

trait Redirects extends Controller {

  def homepageRedirect = CachedAction(MovedPermanently("/"))

  def supporterRedirect = CachedAction {
    MovedPermanently(routes.Info.supporterRedirect(None).path)
  }
}

object Redirects extends Redirects
