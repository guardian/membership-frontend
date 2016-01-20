package controllers

import com.gu.membership.MembershipPlan
import com.gu.memsub.{Subscriber, PaymentStatus, PaidPS}
import com.gu.salesforce._
import model.Benefits
import org.joda.time.{DateTime, Instant}
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json._
import play.api.mvc.Controller
import services.MembersDataAPI
import utils.GuMemCookie

trait User extends Controller {
  val standardFormat = ISODateTimeFormat.dateTime.withZoneUTC
  implicit val writesInstant = Writes[Instant] { instant => JsString(instant.toString(standardFormat)) }

  def me = AjaxSubscriptionAction { implicit request =>
    val json = basicDetails(request.subscriber)
    request.idCookies.foreach(MembersDataAPI.Service.check(request.subscriber))
    Ok(json).withCookies(GuMemCookie.getAdditionCookie(json))
  }

  def basicDetails(subscriber: Subscriber.Member) = Json.obj(
    "userId" -> subscriber.contact.identityId,
    "regNumber" -> subscriber.contact.regNumber,
    "firstName" -> subscriber.contact.firstName,
    "tier" -> subscriber.subscription.plan.tier.name,
    "isPaidTier" -> subscriber.subscription.plan.tier.isPaid,
    "joinDate" -> subscriber.contact.joinDate,
    "benefits" -> Json.obj(
      "discountedEventTickets" -> Benefits.DiscountTicketTiers.contains(subscriber.subscription.plan.tier),
      "complimentaryEventTickets" -> Benefits.ComplimenataryTicketTiers.contains(subscriber.subscription.plan.tier)
    )
  )
}

object User extends User
