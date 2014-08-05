package controllers

import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, Instant}

import scala.concurrent.Future

import play.api.mvc.Controller
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import com.gu.membership.salesforce.Tier

import actions.{AjaxMemberAction, MemberRequest}
import services.StripeService
import model.Stripe

trait User extends Controller {
  def me = AjaxMemberAction { implicit request =>
    Cors(Ok(basicDetails(request)))
  }

  def meDetails = AjaxMemberAction.async { implicit request =>
    request.member.tier match {
      case Tier.Friend =>
        val details = basicDetails(request) ++ Json.obj("subscription" -> Json.obj("plan" -> Json.obj("name" -> "Friend", "amount" -> 0)))
        Future.successful(Cors(Ok(details)))

      case _ => StripeService.Customer.read(request.member.stripeCustomerId.get).map { customer =>
        val paymentDetails = customer.paymentDetails.fold(Json.obj())(extractPaymentDetails)
        Cors(Ok(basicDetails(request) ++ paymentDetails))
      }
    }
  }

  def basicDetails(request: MemberRequest[_]) = {
    val member = request.member

    val standardFormat = ISODateTimeFormat.dateTime.withZoneUTC
    implicit val writesDateTime = Writes[DateTime] { dt => JsString(dt.toString(standardFormat)) }

    Json.obj(
      "userId" -> member.identityId,
      "tier" -> member.tier.toString,
      "joinDate" -> member.joinDate,
      "optIn" -> member.optedIn
    )
  }

  def extractPaymentDetails(paymentDetails: Stripe.PaymentDetails) = {
    val standardFormat = ISODateTimeFormat.dateTime.withZoneUTC
    implicit val writesInstant = Writes[Instant] { instant => JsString(instant.toString(standardFormat)) }

    val Stripe.PaymentDetails(card, subscription) = paymentDetails

    Json.obj(
      "subscription" -> Json.obj(
        "start" -> subscription.start,
        "end" -> subscription.current_period_end,
        "cancelledAt" -> subscription.canceled_at,
        "plan" -> Json.obj(
          "name" -> subscription.plan.name,
          "amount" -> subscription.plan.amount,
          "interval" -> subscription.plan.interval
        ),
        "card" -> Json.obj("last4" -> card.last4, "type" -> card.`type`)
      )
    )
  }
}

object User extends User
