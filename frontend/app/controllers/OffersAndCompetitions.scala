package controllers

import model.ContentItemOffer
import play.api.mvc.Controller
import services.GuardianContentService


trait OffersAndCompetitions extends Controller {
  // TODO move this to CachedAction once this work is ready to go into the wild
  def list = GoogleAuthenticatedStaffAction { implicit request =>
    val results = GuardianContentService.offersAndCompetitionsContent.map(ContentItemOffer)
    Ok(views.html.offer.offersandcomps(results, "Sorry, no matching items were found."))
  }
}

object OffersAndCompetitions extends OffersAndCompetitions
