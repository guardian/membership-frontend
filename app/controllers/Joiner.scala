package controllers

import play.api.mvc.{ Action, Controller }

trait Joiner extends Controller {

  def tierList = Action {
    Ok(views.html.joiner.tierList())
  }

  def friend() = Action {
    Ok(views.html.joiner.tier.friend())
  }

  def partner() = Action {
    Ok(views.html.joiner.tier.partner())
  }

  def patron() = Action {
    Ok(views.html.joiner.tier.patron())
  }

  def paymentFriend() = Action {
    Ok(views.html.joiner.payment.friend())
  }

  def paymentPartner() = Action {
    Ok(views.html.joiner.payment.partner())
  }

  def paymentPatron() = Action {
    Ok(views.html.joiner.payment.patron())
  }

  def thankyouFriend() = Action {
    Ok(views.html.joiner.thankyou.friend())
  }

  def thankyouPartner() = Action {
    Ok(views.html.joiner.thankyou.partner())
  }

  def thankyouPatron() = Action {
    Ok(views.html.joiner.thankyou.patron())
  }

}

object Joiner extends Joiner