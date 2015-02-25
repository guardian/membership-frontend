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
  def offersAndComps = CachedAction { implicit request =>
    Ok(views.html.offer.offersandcomps(contentApiService.membersOnlyContent))
  }
}

object Offer extends Offer
