package controllers

import model.MembersOnlyContent
import play.api.mvc.Controller
import services.GuardianContentService


trait OffersAndCompetitions extends Controller {
  // TODO move this to CachedAction once this work is ready to go into the wild
  def list = GoogleAuthenticatedStaffAction { implicit request =>
    val memberOnlyContent = GuardianContentService.membersOnlyContent.map(MembersOnlyContent)
    Ok(views.html.offer.offersandcomps(memberOnlyContent, "Sorry, no matching events were found."))
  }
}

object OffersAndCompetitions extends OffersAndCompetitions
