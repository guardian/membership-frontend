package controllers

import com.gu.memsub.Subscriber
import model.Benefits
import org.joda.time.Instant
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json._
import play.api.mvc.Controller
import services.{IdentityApi, IdentityService, MembersDataAPI}
import utils.GuMemCookie
import views.support.MembershipCompat._
import scala.concurrent.ExecutionContext.Implicits.global


trait User extends Controller {
  val standardFormat = ISODateTimeFormat.dateTime.withZoneUTC
  implicit val writesInstant = Writes[Instant] { instant => JsString(instant.toString(standardFormat)) }

  def me = AjaxSubscriptionAction { implicit request =>
    val json = basicDetails(request.subscriber)
    Ok(json).withCookies(GuMemCookie.getAdditionCookie(json))
  }

  def checkExistingEmail(email: String) = CachedAction.async { implicit request =>
    for {
      doesUserExist <- IdentityService(IdentityApi).doesUserExist(email)(IdentityRequest(request))
    } yield Ok(Json.obj("emailInUse" -> doesUserExist))
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
      "complimentaryEventTickets" -> Benefits.ComplimentaryTicketTiers.contains(subscriber.subscription.plan.tier)
    )
  )
}

object User extends User
