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
      "joinDate" -> member.joinDate
    )
  }

  def extractPaymentDetails(paymentDetails: Stripe.PaymentDetails) = {
    val standardFormat = ISODateTimeFormat.dateTime.withZoneUTC
    implicit val writesInstant = Writes[Instant] { instant => JsString(instant.toString(standardFormat)) }

    Json.obj(
      "subscription" -> Json.obj(
        "start" -> paymentDetails.subscription.start,
        "end" -> paymentDetails.subscription.current_period_end,
        "plan" -> Json.obj("name" -> paymentDetails.subscription.plan.name, "amount" -> paymentDetails.subscription.plan.amount),
        "card" -> Json.obj("last4" -> paymentDetails.card.last4, "type" -> paymentDetails.card.`type`)
      )
    )
  }
}

object User extends User