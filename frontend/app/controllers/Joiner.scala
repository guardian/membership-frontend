package controllers

import play.api.mvc.{ Action, Controller }
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import actions.AuthenticatedAction
import model.Tier
import services.{ AwsMemberTable, StripeService }
import scala.concurrent.Future

trait Joiner extends Controller {

  def tierList = Action {
    Ok(views.html.joiner.tierList())
  }

  def friend() = Action {
    Ok(views.html.joiner.tier.friend())
  }

  def partner() = Action {
    Ok(views.html.joiner.tier.partner())
  }

  def patron() = Action {
    Ok(views.html.joiner.tier.patron())
  }

  def paymentFriend() = AuthenticatedAction {
    Ok(views.html.joiner.payment.paymentForm(Tier.Friend))
  }

  def paymentPartner() = AuthenticatedAction {
    Ok(views.html.joiner.payment.paymentForm(Tier.Partner))
  }

  def paymentPatron() = AuthenticatedAction {
    Ok(views.html.joiner.payment.paymentForm(Tier.Patron))
  }

  def thankyouFriend() = AuthenticatedAction {
    Ok(views.html.joiner.thankyou.friend())
  }

  def thankyouPartner() = AuthenticatedAction.async { implicit request =>
    AwsMemberTable.get(request.user.id).map { member =>
      StripeService.Customer.read(member.customerId).map { customer =>
        val response = for {
          subscription <- customer.subscriptions.data.headOption
          card <- customer.cards.data.headOption
        } yield Ok(views.html.joiner.thankyou.partner(subscription, card))

        response.getOrElse(NotFound)
      }
    }.getOrElse(Future.successful(BadRequest))
  }

  def thankyouPatron() = AuthenticatedAction.async { implicit request =>
    AwsMemberTable.get(request.user.id).map { member =>
      StripeService.Customer.read(member.customerId).map { customer =>
        val response = for {
          subscription <- customer.subscriptions.data.headOption
          card <- customer.cards.data.headOption
        } yield Ok(views.html.joiner.thankyou.partner(subscription, card))

        response.getOrElse(NotFound)
      }
    }.getOrElse(Future.successful(BadRequest))
  }

}

object Joiner extends Joiner