package controllers

import com.gu.membership.salesforce.Tier
import play.api.mvc.Controller
import play.api.data.Form
import play.api.data.Forms._
import services.{PreMembershipJoiningEventFromSessionExtractor, EventbriteService}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.Future

object Joining extends Controller {

  /*
  *   Tier selection page ===============================================
  */
  def tierChooser() = NoCacheAction { implicit request =>

    val eventOpt = PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(request).flatMap(EventbriteService.getEvent)

    Ok(views.html.joining.tierChooser(eventOpt))
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
