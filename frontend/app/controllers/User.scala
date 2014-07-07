package controllers

import scala.concurrent.Future

import play.api.mvc.Controller
import play.api.libs.json.Json
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import actions.{ MemberRequest, MemberAction }
import services.StripeService
import model.{ Stripe, Tier }

trait User extends Controller {
  def me = MemberAction { implicit request =>
    Cors(Ok(basicDetails(request)))
  }

  def meDetails = MemberAction.async { implicit request =>
    request.member.tier match {
      case Tier.Friend =>
        val details = basicDetails(request) ++ Json.obj("subscription" -> Json.obj("name" -> "Friend", "amount" -> 0))
        Future.successful(Cors(Ok(details)))

      case _ => StripeService.Customer.read(request.member.customerId).map { customer =>
        val subscriptionOpt = for {
          subscription <- customer.subscription
          card <- customer.card
        } yield subscriptionDetails(subscription, card)

        Cors(Ok(basicDetails(request) ++ subscriptionOpt.getOrElse(Json.obj())))
      }
    }
  }

  def basicDetails(request: MemberRequest[_]) =
    Json.obj("userId" -> request.member.userId, "tier" -> request.member.tier.toString)

  def subscriptionDetails(subscription: Stripe.Subscription, card: Stripe.Card) =
     Json.obj(
       "subscription" -> Json.obj(
        "start" -> subscription.start,
        "end" -> subscription.current_period_end,
        "plan" -> Json.obj("name" -> subscription.plan.name, "amount" -> subscription.plan.amount),
        "card" -> Json.obj("last4" -> card.last4, "type" -> card.`type`)
       )
     )
}

object User extends User