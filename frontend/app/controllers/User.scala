package controllers

import org.joda.time.format.ISODateTimeFormat
import org.joda.time.{DateTime, Instant}

import scala.concurrent.Future

import play.api.mvc.Controller
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import com.gu.membership.salesforce.{FreeMember, PaidMember}

import actions.{AjaxMemberAction, MemberRequest}
import services.{StripeService, SubscriptionService}
import model.Stripe

trait User extends Controller {
  val standardFormat = ISODateTimeFormat.dateTime.withZoneUTC
  implicit val writesInstant = Writes[Instant] { instant => JsString(instant.toString(standardFormat)) }

  def me = AjaxMemberAction { implicit request =>
    Cors(Ok(basicDetails(request)))
  }

  def meDetails = AjaxMemberAction.async { implicit request =>
    val futurePaymentDetails = request.member match {
      case paidMember: PaidMember =>
        for {
          customer <- StripeService.Customer.read(paidMember.stripeCustomerId)
          invoice <- SubscriptionService.getInvoiceSummary(paidMember.salesforceAccountId)
        } yield Json.obj(
          "subscription" -> Json.obj(
            "start" -> invoice.startDate,
            "end" -> invoice.endDate,
            "cancelledAt" -> false, // TODO
            "plan" -> Json.obj(
              "name" -> invoice.planName,
              "amount" -> invoice.planAmount * 100,
              "interval" -> (if (invoice.annual) "year" else "month")
            ),
            "card" -> Json.obj("last4" -> customer.card.last4, "type" -> customer.card.`type`)
          )
        )

      case member: FreeMember =>
        val paymentDetails = Json.obj(
          "subscription" -> Json.obj(
            "plan" -> Json.obj("name" -> member.tier.toString, "amount" -> 0)
          )
        )

        Future.successful(paymentDetails)
    }

    futurePaymentDetails.map { paymentDetails => Cors(Ok(basicDetails(request) ++ paymentDetails)) }
  }

  def basicDetails(request: MemberRequest[_]) = {
    val member = request.member

    Json.obj(
      "userId" -> member.identityId,
      "tier" -> member.tier.toString,
      "joinDate" -> member.joinDate,
      "optIn" -> member.optedIn
    )
  }
}

object User extends User
