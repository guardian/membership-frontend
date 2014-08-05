package controllers

import com.gu.membership.salesforce.Tier
import play.api.mvc.Controller
import play.api.data.Form
import play.api.data.Forms._
import services.EventbriteService
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

object Joining extends Controller {

  /*
  *   Tier selection page ===============================================
  */
  def tierChooser() = CachedAction.async { implicit request =>

    val eventService = EventbriteService
    val eventIdOpt = services.PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(request)
    val eventOpt = eventIdOpt.map(eventService.getEvent)

    for (
      event <- Future.sequence(eventOpt.toSeq)
    ) yield {
      Ok(views.html.joining.tierChooser(event.headOption))
    }
  }

  private val tierForm = Form { single("tier" -> nonEmptyText) }

  def tierChooserRedirect() = CachedAction { implicit request =>

    def redirect(formData: (String)) = {
      val tierString = formData
      Redirect(routes.Joiner.enterDetails(Tier.routeMap(tierString)))
    }

    tierForm.bindFromRequest.fold(_ => BadRequest, redirect)
  }

}
