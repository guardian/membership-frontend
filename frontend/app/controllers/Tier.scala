package controllers

import play.api.mvc.Controller
import actions.{AuthenticatedAction, MemberAction}

trait Tier extends Controller {

  def change()  = AuthenticatedAction { implicit request =>
    Ok(views.html.tier.change())
  }

  def confirmDowngrade() = AuthenticatedAction { implicit request =>
    Ok(views.html.tier.downgrade.confirm())
  }

  def downgradeSummary() = AuthenticatedAction { implicit request =>
    Ok(views.html.tier.downgrade.summary())
  }

  def downgradeTier() = AuthenticatedAction { implicit request =>
    Redirect("/tier/downgrade/summary")
  }

  def confirmCancel() = AuthenticatedAction { implicit request =>
    Ok(views.html.tier.cancel.confirm())
  }

  def cancelSummary() = AuthenticatedAction { implicit request =>
    Ok(views.html.tier.cancel.summary())
  }

  def cancelTier() = AuthenticatedAction { implicit request =>
    Redirect("/tier/cancel/summary")
  }
}

object Tier extends Tier
