package controllers

import com.gu.membership.salesforce._
import model.Benefits
import org.joda.time.Instant
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json._
import play.api.mvc.Controller
import services.{CASService, MembersDataAPI}
import utils.GuMemCookie

trait User extends Controller {
  val standardFormat = ISODateTimeFormat.dateTime.withZoneUTC
  implicit val writesInstant = Writes[Instant] { instant => JsString(instant.toString(standardFormat)) }

  val casService = CASService

  def me = AjaxMemberAction { implicit request =>
    val json = basicDetails(request.member)
    request.idCookies.foreach(MembersDataAPI.Service.check(request.member.memberStatus))
    Ok(json).withCookies(GuMemCookie.getAdditionCookie(json))
  }

  def basicDetails(member: Contact[Member, PaymentMethod]) = Json.obj(
    "userId" -> member.identityId,
    "regNumber" -> member.memberStatus.regNumberLabel,
    "firstName" -> member.firstName,
    "tier" -> member.tier.name,
    "isPaidTier" -> member.tier.isPaid,
    "joinDate" -> member.joinDate,
    "benefits" -> Json.obj(
      "discountedEventTickets" -> Benefits.DiscountTicketTiers.contains(member.tier),
      "complimentaryEventTickets" -> Benefits.ComplimenataryTicketTiers.contains(member.tier)
    )
  )
}

object User extends User
