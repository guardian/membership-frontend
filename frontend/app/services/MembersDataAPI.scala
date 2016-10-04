package services

import actions.ActionRefiners.SubReqWithSub
import com.gu.identity.play.AccessCredentials
import com.gu.memsub.Subscriber.Member
import com.gu.memsub.util.WebServiceHelper
import com.gu.okhttp.RequestRunners
import com.gu.okhttp.RequestRunners._
import com.gu.salesforce.Tier
import configuration.Config
import monitoring.MembersDataAPIMetrics
import okhttp3.Request
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.mvc.Cookie
import views.support.MembershipCompat._

import scala.concurrent.Future
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

  object Attributes {
    def fromMember(member: Member) = Attributes(member.subscription.plan.tier, member.contact.regNumber)
  }

  case class ApiError(message: String, details: String) extends RuntimeException(s"$message - $details")

  implicit val errorReads: Reads[ApiError] = (
    (JsPath \ "message").read[String] and
    (JsPath \ "details").read[String]
  )(ApiError)

  case class Helper(cookies: Seq[Option[Cookie]]) extends WebServiceHelper[Attributes, ApiError] {
    override val wsUrl: String = Config.membersDataAPIUrl
    override def wsPreExecute(req: Request.Builder): Request.Builder = req.addHeader(
      "Cookie", cookies.flatten.map(c => s"${c.name}=${c.value}").mkString("; "))

    override val httpClient: LoggingHttpClient[Future] = RequestRunners.loggingRunner(MembersDataAPIMetrics)

  }

  object Service  {
    def checkMatchesResolvedMemberIn(memberRequest: SubReqWithSub[_]) = memberRequest.user.credentials match {
      case cookies: AccessCredentials.Cookies =>
        get(Seq(memberRequest.cookies.get("GU_U"), memberRequest.cookies.get("SC_GU_U"))).onComplete {
          case Success(memDataApiAttrs) =>
            val prefix = s"members-data-api-check identity=${memberRequest.user.id} salesforce=${memberRequest.subscriber.contact.salesforceContactId} : "
            val salesforceAttrs = Attributes.fromMember(memberRequest.subscriber)
            if (memDataApiAttrs != salesforceAttrs) {
              val message = s"$prefix MISMATCH salesforce=$salesforceAttrs mem-data-api=$memDataApiAttrs"
              if (memDataApiAttrs.tier != salesforceAttrs.tier) Logger.error(message) else Logger.warn(message)
              MembersDataAPIMetrics.put("members-data-api-mismatch", 1)
            } else {
              Logger.debug(s"$prefix MATCH")
              MembersDataAPIMetrics.put("members-data-api-match", 1)
            }
          case Failure(err) => Logger.error(s"Failed to get membership attributes from membership-data-api for user ${memberRequest.user.id} (OK in dev)", err)
        }
      case _ => Logger.error(s"Unexpected credentials! ${memberRequest.user.credentials}")
    }

    private def get(cookies: Seq[Option[Cookie]]) = Helper(cookies).get[Attributes]("user-attributes/me/membership")
  }
}
