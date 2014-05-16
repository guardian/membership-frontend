package controllers

import play.api.mvc.{ Action, Controller }

trait Joiner extends Controller {

  def tierList = Action {
    Ok(views.html.joiner.tierList())
  }

  def tier(memberTier: String) = Action {
    Ok(views.html.joiner.tier(memberTier))
  }

  def payment(memberTier: String) = Action {
    Ok(views.html.joiner.payment(memberTier))
  }

  def thankyou(memberTier: String) = Action {
    Ok(views.html.joiner.thankyou(memberTier))
  }

}

object Joiner extends Joiner