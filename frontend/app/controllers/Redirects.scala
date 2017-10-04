package controllers

import play.api.mvc._

trait Redirects extends Controller {

  def homepageRedirect = CachedAction(MovedPermanently("/"))

  def supporterRedirect = CachedAction {
    MovedPermanently(routes.Info.supporterRedirect(None).path)
  }

  def supportRedirect = CachedAction(MovedPermanently("https://support.theguardian.com/"))
}

object Redirects extends Redirects
