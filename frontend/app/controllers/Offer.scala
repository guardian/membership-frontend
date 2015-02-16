package controllers

import actions.Functions._
import configuration.Config
import play.api.mvc.Controller

trait Offer extends Controller {
  val permanentStaffGroups = Config.staffAuthorisedEmailGroups
  val AuthorisedStaff = GoogleAuthenticatedStaffAction andThen isInAuthorisedGroupGoogleAuthReq(
    permanentStaffGroups, views.html.fragments.oauth.staffWrongGroup())

  // TODO move this to CachedAction once this work is ready to go into the wild
  def subscriber = AuthorisedStaff { implicit request =>
    Ok(views.html.offer.subscriber())
  }
}

object Offer extends Offer
