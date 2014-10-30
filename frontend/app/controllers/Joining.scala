package controllers

import play.api.mvc.Controller
import play.api.data.Form
import play.api.data.Forms._

import com.gu.membership.salesforce.Tier

import model.PageInfo
import configuration.CopyConfig
import services.{GuardianLiveEventService, PreMembershipJoiningEventFromSessionExtractor}

object Joining extends Controller {

  /*
  *   Tier selection page ===============================================
  */
  def tierChooser() = NoCacheAction { implicit request =>

    val eventOpt = PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(request).flatMap(GuardianLiveEventService.getEvent)
    val pageInfo = PageInfo(
      CopyConfig.copyTitleChooseTier,
      request.path,
      Some(CopyConfig.copyDescriptionChooseTier)
    )
    Ok(views.html.joining.tierChooser(eventOpt, pageInfo))
  }

  private val tierForm = Form { single("tier" -> nonEmptyText) }

  def tierChooserRedirect() = NoCacheAction { implicit request =>

    def redirect(formData: (String)) = {
      val tierString = formData
      Redirect(routes.Joiner.enterDetails(Tier.routeMap(tierString)))
    }

    tierForm.bindFromRequest.fold(_ => BadRequest, redirect)
  }

}
