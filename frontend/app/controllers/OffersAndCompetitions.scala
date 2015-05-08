package controllers

import play.api.mvc.Controller
import services.GuardianContentService
import model.ContentItemOffer

trait OffersAndCompetitions extends Controller {
  def list = CachedAction { implicit request =>
    val results = GuardianContentService.offersAndCompetitionsContent.map(ContentItemOffer)
      .filter { item =>
        item.content.fields.map(_("membershipAccess")).isEmpty
      }.filter(_.imgOpt.nonEmpty)
    Ok(views.html.offer.offersandcomps(results))
  }
}

object OffersAndCompetitions extends OffersAndCompetitions
