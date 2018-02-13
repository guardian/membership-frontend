package controllers

import actions.{CommonActions, TouchpointCommonActions}
import com.gu.memsub.Subscriber
import model.Benefits
import org.joda.time.Instant
import org.joda.time.format.ISODateTimeFormat
import play.api.libs.json._
import play.api.mvc.{BaseController, ControllerComponents}
import play.cache.CachedAction
import services.{IdentityApi, IdentityService, MembersDataAPI}
import utils.GuMemCookie
import views.support.MembershipCompat._

import scala.concurrent.ExecutionContext

class User(val identityApi: IdentityApi, touchpointCommonActions: TouchpointCommonActions, implicit val executionContext: ExecutionContext, commonActions: CommonActions, membersDataAPI: MembersDataAPI, override protected val controllerComponents: ControllerComponents) extends BaseController {
  val standardFormat = ISODateTimeFormat.dateTime.withZoneUTC
  implicit val writesInstant = Writes[Instant] { instant => JsString(instant.toString(standardFormat)) }

  import touchpointCommonActions._
  import commonActions.CachedAction

  def me = AjaxSubscriptionAction { implicit request =>
    val json = basicDetails(request.subscriber)
    membersDataAPI.Service.checkMatchesResolvedMemberIn(request)
    Ok(json).withCookies(GuMemCookie.getAdditionCookie(json))
  }

  def checkExistingEmail(email: String) = CachedAction.async { implicit request =>
    for {
      doesUserExist <- IdentityService(identityApi).doesUserExist(email)(IdentityRequest(request))
    } yield Ok(Json.obj("emailInUse" -> doesUserExist))
  }

  def basicDetails(subscriber: Subscriber.Member) = Json.obj(
    "userId" -> subscriber.contact.identityId,
    "regNumber" -> subscriber.contact.regNumber,
    "firstName" -> subscriber.contact.firstName,
    "tier" -> subscriber.subscription.plan.tier.name,
    "isPaidTier" -> subscriber.subscription.plan.tier.isPaid,
    "joinDate" -> subscriber.contact.joinDate.toInstant,
    "benefits" -> Json.obj(
      "discountedEventTickets" -> Benefits.DiscountTicketTiers.contains(subscriber.subscription.plan.tier),
      "complimentaryEventTickets" -> Benefits.ComplimentaryTicketTiers.contains(subscriber.subscription.plan.tier)
    )
  )
}
