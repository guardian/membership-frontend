package controllers

import play.api.mvc.Controller

trait Redirects extends Controller {

  def homepageRedirect = CachedAction(MovedPermanently("/"))

}

object Redirects extends Redirects
