package controllers

import play.api.mvc.Controller

trait Redirects extends Controller {

  private def homepageRedirect = MovedPermanently("/")

  // About
  def about = CachedAction(homepageRedirect)

  // A/B Join Challenger page
  def joinChallenger = CachedAction(homepageRedirect)

  // Join / Pricing page
  def join = CachedAction(homepageRedirect)

  // Supporter
  def supporter = CachedAction {
    MovedPermanently(routes.Info.supporter.toString)
  }

}

object Redirects extends Redirects
