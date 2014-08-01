package controllers

import actions.{PaidMemberAction, MemberAction, AuthenticatedAction}
import com.gu.membership.salesforce.Tier
import model.Eventbrite
import play.api.Routes
import play.api.mvc.Controller
import play.api.data.Form
import play.api.data.Forms._
import play.core.Router
import services.{StripeService, AuthenticationService}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object Joining extends Controller {

  /*
  *   Tier selection page ===============================================
  */
  def tierChooser() = CachedAction { implicit request =>

    val eventIdOpt = services.PreMembershipJoiningEventFromSessionExtractor.eventIdFrom(request)


    Ok(views.html.joining.tierChooser(eventIdOpt))
  }

  private val tierForm = Form { single("tier" -> nonEmptyText) }

  def tierChooserRedirect() = CachedAction { implicit request =>

    def redirect(formData: (String)) = {
      val tierString = formData
      Redirect(routes.Joiner.enterDetails(tierString)) //TODO handle friend?
    }

    tierForm.bindFromRequest.fold(_ => BadRequest, redirect)
  }

}
