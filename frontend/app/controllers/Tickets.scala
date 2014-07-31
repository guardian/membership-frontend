package controllers

import actions.{PaidMemberAction, MemberAction, AuthenticatedAction}
import com.gu.membership.salesforce.Tier
import play.api.mvc.Controller
import play.api.data.Form
import play.api.data.Forms._
import services.{StripeService, AuthenticationService}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

trait Tickets extends Controller {

  val authService: AuthenticationService

  /*
  *   Tier selection page ===============================================
  */

  def ticketsJoin(ticketId: String) = CachedAction { implicit request =>
    Ok(views.html.tickets.ticketsJoin(ticketId))
  }

}

object Tickets extends Tickets {
  val authService = AuthenticationService
}
