package controllers

import play.api.mvc.Controller
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import actions.{AuthenticatedAction, MemberAction}
import services.StripeService

trait Tier extends Controller {

  def change()  = AuthenticatedAction { implicit request =>
    Ok(views.html.tier.change())
  }

  def confirmDowngrade() = AuthenticatedAction { implicit request =>
    Ok(views.html.tier.downgrade.confirm())
  }

  def downgradeSummary() = MemberAction.async { implicit request =>
    StripeService.Customer.read(request.member.customerId).map { customer =>
      val response = for {
        subscription <- customer.subscriptions.data.headOption
        card <- customer.cards.data.headOption
      } yield Ok(views.html.tier.downgrade.summary(subscription, card))

      response.getOrElse(NotFound)
    }
  }
}

object Tier extends Tier
