package controllers

import com.gu.membership.salesforce.{FreeMember, Member, PaidMember}
import org.joda.time.Instant
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc.{Controller, Cookie}
import utils.GuMemCookie

import scala.concurrent.Future

trait User extends Controller {
  val standardFormat = ISODateTimeFormat.dateTime.withZoneUTC
  implicit val writesInstant = Writes[Instant] { instant => JsString(instant.toString(standardFormat)) }

  def me = AjaxMemberAction { implicit request =>
    val json = basicDetails(request.member)
    Ok(json).withCookies(Cookie("GU_MEM", GuMemCookie.encodeUserJson(json), secure = true, httpOnly = false))
  }

  def meDetails = AjaxMemberAction.async { implicit request =>
    def futureCardDetails = request.member match {
      case paidMember: PaidMember =>
        for {
          customer <- request.touchpointBackend.stripeService.Customer.read(paidMember.stripeCustomerId)
        } yield Json.obj("card" -> Json.obj("last4" -> customer.card.last4, "type" -> customer.card.`type`))

      case member: FreeMember =>
        Future.successful(Json.obj())
    }

    val futurePaymentDetails = for {
      cardDetails <- futureCardDetails
      subscriptionStatus <- request.touchpointBackend.subscriptionService.getSubscriptionStatus(request.member)
      subscriptionDetails <- request.touchpointBackend.subscriptionService.getSubscriptionDetails(subscriptionStatus.current)
    } yield Json.obj(
      "optIn" -> !subscriptionStatus.cancelled,
      "subscription" -> (cardDetails ++ Json.obj(
        "start" -> subscriptionDetails.startDate,
        "end" -> subscriptionDetails.endDate,
        "cancelledAt" -> subscriptionStatus.future.isDefined,
        "plan" -> Json.obj(
          "name" -> subscriptionDetails.planName,
          "amount" -> subscriptionDetails.planAmount * 100,
          "interval" -> (if (subscriptionDetails.annual) "year" else "month")
        ))
      )
    )

    futurePaymentDetails.map { paymentDetails => Ok(basicDetails(request.member) ++ paymentDetails) }
  }

  def basicDetails(member: Member) = Json.obj(
    "userId" -> member.identityId,
    "regNumber" -> member.regNumber.mkString,
    "firstName" -> member.firstName,
    "tier" -> member.tier.name,
    "joinDate" -> member.joinDate
  )
}

object User extends User
