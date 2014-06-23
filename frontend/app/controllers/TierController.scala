package controllers

import scala.concurrent.Future

import play.api.mvc.Controller
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import actions.{AuthenticatedAction, MemberAction}
import services.StripeService

trait TierController extends Controller {

  def change()  = AuthenticatedAction { implicit request =>
    Ok(views.html.tier.change())
  }

  def confirmDowngrade() = AuthenticatedAction { implicit request =>
    Ok(views.html.tier.downgrade.confirm())
  }

  def downgradeSummary() = MemberAction.async { implicit request =>
    StripeService.Customer.read(request.member.customerId).map { customer =>
      val response = for {
        subscription <- customer.subscription
        card <- customer.card
      } yield Ok(views.html.tier.downgrade.summary(subscription, card))

      response.getOrElse(NotFound)
    }
  }

  def downgradeTier() = MemberAction.async { implicit request =>
      for {
        customer <- StripeService.Customer.read(request.member.customerId)
        cancelledOpt = customer.subscription.map { subscription =>
          StripeService.Subscription.delete(customer.id, subscription.id)
        }
        cancelled <- Future.sequence(cancelledOpt.toSeq)
      } yield {
        cancelled.headOption.map(_ => Redirect("/tier/downgrade/summary")).getOrElse(NotFound)
      }
  }

  def confirmCancel() = AuthenticatedAction { implicit request =>
    Ok(views.html.tier.cancel.confirm())
  }

  def cancelSummary() = MemberAction.async { implicit request =>
    StripeService.Customer.read(request.member.customerId).map { customer =>
      val response = for {
        subscription <- customer.subscription
        card <- customer.card
      } yield Ok(views.html.tier.cancel.summary(subscription, card))

      response.getOrElse(NotFound)
    }
  }

  def cancelTier() = AuthenticatedAction { implicit request =>
    Redirect("/tier/cancel/summary")
  }
}

object TierController extends TierController
