package controllers

import play.api.mvc.Controller
import model.PageInfo
import configuration.CopyConfig
import services.{EventbriteService, PreMembershipJoiningEventFromSessionExtractor}

object Joining extends Controller {

  /*
  *   Tier selection page ===============================================
  */
  def tierChooser() = NoCacheAction { implicit request =>

    val eventOpt = PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(request).flatMap(EventbriteService.getBookableEvent)
    val pageInfo = PageInfo(
      CopyConfig.copyTitleChooseTier,
      request.path,
      Some(CopyConfig.copyDescriptionChooseTier)
    )
    Ok(views.html.joining.tierChooser(eventOpt, pageInfo))
  }

}
