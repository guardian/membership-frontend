package controllers

import org.apache.commons.codec.binary.Base64

import org.joda.time.format.ISODateTimeFormat
import org.joda.time.Instant

import scala.concurrent.Future

import play.api.mvc.{Cookie, Controller}
import play.api.libs.json._
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import com.gu.membership.salesforce.{Member, FreeMember, PaidMember}

trait User extends Controller {
  val standardFormat = ISODateTimeFormat.dateTime.withZoneUTC
  implicit val writesInstant = Writes[Instant] { instant => JsString(instant.toString(standardFormat)) }

  def me = AjaxMemberAction { implicit request =>
    val json = basicDetails(request.member)
    Ok(json).withCookies(Cookie("GU_MEM", Base64.encodeBase64String(Json.stringify(json).getBytes), secure = true, httpOnly = false))
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
      subscriptionStatus <- request.touchpointBackend.subscriptionService.getSubscriptionStatus(request.member.salesforceAccountId)
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
    "regNumber" -> member.regNumber,
    "firstName" -> member.firstName,
    "tier" -> member.tier.toString,
    "joinDate" -> member.joinDate
  )
}

object User extends User
