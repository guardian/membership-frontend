package services

import actions.ActionRefiners.SubReqWithSub
import com.gu.identity.play.AccessCredentials
import com.gu.memsub.Subscriber.Member
import com.gu.memsub.util.WebServiceHelper
import com.gu.okhttp.RequestRunners
import com.gu.okhttp.RequestRunners._
import com.gu.salesforce.Tier
import configuration.Config
import monitoring.DummyMetrics
import okhttp3.Request
import com.gu.monitoring.SafeLogger
import com.gu.monitoring.SafeLogger._
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import views.support.MembershipCompat._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

object MembersDataAPI {
  implicit val tierReads = Reads[Tier] {
    case JsString(s) => Tier.slugMap.get(s.toLowerCase).map(JsSuccess(_)).getOrElse(JsError(s"Unknown tier $s"))
    case _ => JsError("Expected a string representation of a tier")
  }

  implicit val attributesReads: Reads[Attributes] = (
    (JsPath \ "tier").read[Tier] and
      (JsPath \ "membershipNumber").readNullable[String]
    )(Attributes.apply _)

  case class Attributes(tier: Tier, membershipNumber: Option[String])
  case class Behaviour(userId: String, activity: Option[String], lastObserved: Option[String], note: Option[String], emailed: Option[Boolean])

  object Attributes {
    def fromMember(member: Member) = Attributes(member.subscription.plan.tier, member.contact.regNumber)
  }

  case class ApiError(message: String, details: String) extends RuntimeException(s"$message - $details")

  implicit val errorReads: Reads[ApiError] = (
    (JsPath \ "message").read[String] and
      (JsPath \ "details").read[String]
    )(ApiError)

}

class MembersDataAPI(executionContext: ExecutionContext) {

  private implicit val ec = executionContext

  import MembersDataAPI._

  private case class AttributeHelper(accessCredentials: AccessCredentials.Cookies) extends WebServiceHelper[Attributes, ApiError] {
    override val wsUrl: String = Config.membersDataAPIUrl
    override def wsPreExecute(req: Request.Builder): Request.Builder = {
      req.addHeader("Cookie", accessCredentials.cookies.map(c => s"${c.name}=${c.value}").mkString("; "))
    }
    override val httpClient: LoggingHttpClient[Future] = RequestRunners.loggingRunner(DummyMetrics)
  }

  object Service  {

    def checkMatchesResolvedMemberIn(memberRequest: SubReqWithSub[_]) = memberRequest.user.credentials match {
      case cookies: AccessCredentials.Cookies =>
        getAttributes(cookies).onComplete {
          case Success(memDataApiAttrs) =>
            val salesforceAttrs = Attributes.fromMember(memberRequest.subscriber)
            if (memDataApiAttrs.tier != salesforceAttrs.tier) {
              SafeLogger.error(scrub"${memberRequest.user.id} Salesforce and members-data-api had differing Tier info: salesforce=${salesforceAttrs.tier} mem-data-api=${memDataApiAttrs.tier}")
            }
          case Failure(err) => SafeLogger.error(scrub"Failed to get membership attributes from membership-data-api for user ${memberRequest.user.id} (OK in dev)", err)
        }
      case _ => SafeLogger.error(scrub"Unexpected credentials for getAttributes! ${memberRequest.user.credentials}")
    }

    private def getAttributes(cookies: AccessCredentials.Cookies) = AttributeHelper(cookies).get[Attributes]("user-attributes/me/membership")

  }
}
