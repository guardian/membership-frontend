package controllers

import actions.{PaidMemberAction, MemberAction, AuthenticatedAction}
import com.gu.membership.salesforce.Tier
import play.api.mvc.Controller
import play.api.data.Form
import play.api.data.Forms._
import services.{StripeService, AuthenticationService}
import play.api.libs.concurrent.Execution.Implicits.defaultContext

object Tickets extends Controller {

  /*
  *   Thankyou pages ====================================================
  */

  def ticketsThankyou(ticketId: String, tierString: String) = MemberAction { implicit request =>
    Tier.routeMap(tierString) match {
      case Tier.Friend => Redirect(routes.Tickets.ticketsThankyouFriend(ticketId))
      case Tier.Partner => Redirect(routes.Tickets.ticketsThankyouPartner(ticketId))
      case Tier.Patron => Redirect(routes.Tickets.ticketsThankyouPatron(ticketId))
      case _ => Forbidden
    }
  }

  def ticketsThankyouFriend(ticketId: String) = MemberAction { implicit request =>
    Ok(views.html.tickets.thankyouFriendAndBuyTickets(ticketId))
  }

  def ticketsThankyouPartner(ticketId: String) = PaidMemberAction.async { implicit request =>
    StripeService.Customer.read(request.stripeCustomerId).map { customer =>
      val response = for {
        paymentDetails <- customer.paymentDetails
      } yield Ok(views.html.tickets.thankyouPaidAndBuyTickets(ticketId, paymentDetails))

      response.getOrElse(NotFound)
    }
  }

  def ticketsThankyouPatron(ticketId: String) = PaidMemberAction.async { implicit request =>
    StripeService.Customer.read(request.stripeCustomerId).map { customer =>
      val response = for {
        paymentDetails <- customer.paymentDetails
      } yield Ok(views.html.tickets.thankyouPaidAndBuyTickets(ticketId, paymentDetails))

      response.getOrElse(NotFound)
    }
  }

}
