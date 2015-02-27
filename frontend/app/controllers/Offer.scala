package controllers

import actions.Functions._
import configuration.Config
import play.api.mvc.Controller
import services.GuardianContentService

trait Offer extends Controller {
  val permanentStaffGroups = Config.staffAuthorisedEmailGroups
  val AuthorisedStaff = GoogleAuthenticatedStaffAction andThen isInAuthorisedGroupGoogleAuthReq(
    permanentStaffGroups, views.html.fragments.oauth.staffWrongGroup())
  val contentApiService = GuardianContentService

  // TODO move this to CachedAction once this work is ready to go into the wild
  def subscriber = AuthorisedStaff { implicit request =>
    Ok(views.html.offer.subscriber())
  }

  // TODO FYI this controller was originally meant for the subscribers landing page - maybe we need a rethink on the names
  // TODO of these controllers as I feel these methods don't belong together
  def offersAndComps = CachedAction { implicit request =>
    Ok(views.html.offer.offersandcomps(contentApiService.membersOnlyContent, "Sorry, no matching events were found."))
  }
}

object Offer extends Offer
