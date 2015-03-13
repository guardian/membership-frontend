package controllers

import actions.Functions._
import configuration.Config
import model.MembersOnlyContent
import play.api.mvc.Controller
import services.GuardianContentService


trait OffersAndCompetitions extends Controller {
  val contentApiService = GuardianContentService
  val permanentStaffGroups = Config.staffAuthorisedEmailGroups
  val AuthorisedStaff = GoogleAuthenticatedStaffAction andThen isInAuthorisedGroupGoogleAuthReq(
    permanentStaffGroups, views.html.fragments.oauth.staffWrongGroup())

  def list = AuthorisedStaff { implicit request =>
    val memberOnlyContent = contentApiService.membersOnlyContent.map(MembersOnlyContent)
    Ok(views.html.offer.offersandcomps(memberOnlyContent, "Sorry, no matching events were found."))
  }
}

object OffersAndCompetitions extends OffersAndCompetitions
