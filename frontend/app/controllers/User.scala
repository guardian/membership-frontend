package controllers

import com.gu.cas.CAS.CASSuccess
import com.gu.membership.salesforce.{FreeMember, Member, PaidMember}
import model.PaidTiers
import org.joda.time.{DateTime, Instant}
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc.{Controller, Cookie}
import services.CASService
import utils.GuMemCookie
import actions._

import scala.concurrent.Future

trait User extends Controller {
  val standardFormat = ISODateTimeFormat.dateTime.withZoneUTC
  implicit val writesInstant = Writes[Instant] { instant => JsString(instant.toString(standardFormat)) }

  val casService = CASService

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
    "isPaidTier" -> PaidTiers.isPaid(member.tier),
    "joinDate" -> member.joinDate
  )

  def checkSubscriberDetails(id: String, postcode: Option[String], lastName: String) = AjaxAuthenticatedAction.async { implicit request =>
    def json(id: String, isValid: Boolean, errorMsg: Option[String]) = {
      Json.obj("subscriber-id" -> id, "valid" -> isValid) ++ errorMsg.map(msg => Json.obj("msg" -> msg)).getOrElse(Json.obj())
    }

    for {
      casResult <- casService.check(id, postcode, lastName)
      casIdNotUsed <- request.touchpointBackend.subscriptionService.getSubscriptionsByCasId(id)
    }
    yield {
      casResult match {
        case success: CASSuccess => {
          if (new DateTime(success.expiryDate).isBeforeNow()) Ok(json(id, false, Some("Sorry, your subscription has expired.")))
          else if(casIdNotUsed.nonEmpty) Ok(json(id, false, Some(s"Sorry, the subscriber account number entered has already been used to redeem this offer.")))
          else Ok(Json.obj("subscriber-id" -> id, "valid" -> true))
        }
        case _ => Ok(json(id, false, Some(s"To redeem this offer we need more information to validate your subscriber account number.  Please review the additional information required and try again.")))
      }
    }
  }
}

object User extends User
