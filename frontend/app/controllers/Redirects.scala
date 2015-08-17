package controllers

import play.api.mvc.Controller

trait Redirects extends Controller {

  // Subscriber Offer
  def subscriberOffer = CachedAction {
    MovedPermanently("/")
  }

}

object Redirects extends Redirects
