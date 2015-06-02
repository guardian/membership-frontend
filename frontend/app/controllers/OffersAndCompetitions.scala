package controllers

import play.api.mvc.Controller
import services.GuardianContentService
import model.ContentItemOffer

trait OffersAndCompetitions extends Controller {
  def list = CachedAction { implicit request =>

    val results = GuardianContentService.offersAndCompetitionsContent.map(ContentItemOffer)
      .filter(_.content.fields.map(_("membershipAccess")).isEmpty)
      .filter(!_.content.webTitle.startsWith("EXPIRED"))
      .filter(_.imgOpt.nonEmpty)

    Ok(views.html.offer.offersandcomps(results))
  }
}

object OffersAndCompetitions extends OffersAndCompetitions
