package controllers

import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, Instant}

import scala.concurrent.Future

import play.api.mvc.Controller
import play.api.libs.json._
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
        val details = basicDetails(request) ++ Json.obj("subscription" -> Json.obj("plan" -> Json.obj("name" -> "Friend", "amount" -> 0)))
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

  def basicDetails(request: MemberRequest[_]) = {
    val member = request.member

    implicit object JodaDateTimeWriter extends Writes[DateTime] {
      val standardFormat = ISODateTimeFormat.dateTime.withZoneUTC

      def writes(dateTime: DateTime) = JsString(dateTime.toString(standardFormat))
    }
    
    Json.obj(
      "userId" -> member.userId,
      "tier" -> member.tier.toString,
      "joinDate" -> member.joinDate,
      "optIn" -> false
    )
  }

  def subscriptionDetails(subscription: Stripe.Subscription, card: Stripe.Card) =
     Json.obj(
       "subscription" -> Json.obj(
        "start" -> subscription.start,
        "end" -> subscription.current_period_end,
        "cancelledAt" -> subscription.canceled_at,
        "plan" -> Json.obj("name" -> subscription.plan.name, "amount" -> subscription.plan.amount, "interval" -> subscription.plan.interval),
        "card" -> Json.obj("last4" -> card.last4, "type" -> card.`type`)
       )
     )
}

object User extends User