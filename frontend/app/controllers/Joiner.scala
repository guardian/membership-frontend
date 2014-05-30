package controllers

import play.api.mvc.Controller
import actions.AuthenticatedAction
import model.Tier

trait Joiner extends Controller {

  def tierList = CachedAction {
    Ok(views.html.joiner.tierList())
  }

  def friend() = CachedAction {
    Ok(views.html.joiner.tier.friend())
  }

  def partner() = CachedAction {
    Ok(views.html.joiner.tier.partner())
  }

  def patron() = CachedAction {
    Ok(views.html.joiner.tier.patron())
  }

  def paymentFriend() = AuthenticatedAction {
    Ok(views.html.joiner.payment.paymentForm(Tier.Friend))
  }

  def paymentPartner() = AuthenticatedAction {
    Ok(views.html.joiner.payment.paymentForm(Tier.Partner))
  }

  def paymentPatron() = AuthenticatedAction {
    Ok(views.html.joiner.payment.paymentForm(Tier.Patron))
  }

  def thankyouFriend() = AuthenticatedAction {
    Ok(views.html.joiner.thankyou.friend())
  }

  def thankyouPartner() = AuthenticatedAction {
    Ok(views.html.joiner.thankyou.partner())
  }

  def thankyouPatron() = AuthenticatedAction {
    Ok(views.html.joiner.thankyou.patron())
  }

}

object Joiner extends Joiner