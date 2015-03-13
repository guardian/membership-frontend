package controllers

import model.MembersOnlyContent
import play.api.mvc.Controller
import services.GuardianContentService


trait OffersAndCompetitions extends Controller {
  val contentApiService = GuardianContentService

  def list = CachedAction { implicit request =>
    val memberOnlyContent = contentApiService.membersOnlyContent.map(MembersOnlyContent)
    Ok(views.html.offer.offersandcomps(memberOnlyContent, "Sorry, no matching events were found."))
  }
}

object OffersAndCompetitions extends OffersAndCompetitions
