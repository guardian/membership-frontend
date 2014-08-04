package controllers

import actions.{PaidMemberAction, MemberAction, AuthenticatedAction}
import com.gu.membership.salesforce.Tier
import play.api.mvc.Controller
import play.api.data.Form
import play.api.data.Forms._
import services.{StripeService, AuthenticationService}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object Joining extends Controller {

  /*
  *   Tier selection page ===============================================
  */
  def tierChooser() = CachedAction { implicit request =>
    Ok(views.html.joining.tierChooser())
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
