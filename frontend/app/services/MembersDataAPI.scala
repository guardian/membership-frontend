package services

import com.gu.identity.play.{AccessCredentials, AuthenticatedIdUser}
import com.gu.memsub.Subscriber.Member
import com.gu.memsub.util.WebServiceHelper
import com.gu.okhttp.RequestRunners
import com.gu.okhttp.RequestRunners._
import com.gu.salesforce.Tier
import configuration.Config
import monitoring.MembersDataAPIMetrics
import okhttp3.Request
import org.joda.time.DateTime
import org.joda.time.format.ISODateTimeFormat
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
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
  case class Behaviour(userId: String, activity: Option[String], lastObserved: Option[String], note: Option[String], emailed: Option[Boolean])

  object Attributes {
    def fromMember(member: Member) = Attributes(member.subscription.plan.tier, member.contact.regNumber)
  }

  case class ApiError(message: String, details: String) extends RuntimeException(s"$message - $details")

  implicit val errorReads: Reads[ApiError] = (
    (JsPath \ "message").read[String] and
    (JsPath \ "details").read[String]
  )(ApiError)

  implicit val behaviourReads: Reads[Behaviour] = (
    (JsPath \ "userId").read[String] and
    (JsPath \ "activity").readNullable[String] and
    (JsPath \ "lastObserved").readNullable[String] and
    (JsPath \ "note").readNullable[String] and
    (JsPath \ "emailed").readNullable[Boolean]
  )(Behaviour.apply _)

  case class AttributeHelper(accessCredentials: AccessCredentials.Cookies) extends WebServiceHelper[Attributes, ApiError] {
    override val wsUrl: String = Config.membersDataAPIUrl
    override def wsPreExecute(req: Request.Builder): Request.Builder = {
      req.addHeader("Cookie", accessCredentials.cookies.map(c => s"${c.name}=${c.value}").mkString("; "))
    }
    override val httpClient: LoggingHttpClient[Future] = RequestRunners.loggingRunner(MembersDataAPIMetrics)
  }

  case class BehaviourHelper(accessCredentials: AccessCredentials.Cookies) extends WebServiceHelper[Behaviour, ApiError] {
    override val wsUrl: String = Config.membersDataAPIUrl
    override def wsPreExecute(req: Request.Builder): Request.Builder = {
      req.addHeader("Cookie", accessCredentials.cookies.map(c => s"${c.name}=${c.value}").mkString("; "))
    }
    override val httpClient: LoggingHttpClient[Future] = RequestRunners.loggingRunner(MembersDataAPIMetrics)
  }

  object Service  {

    def upsertBehaviour(user: AuthenticatedIdUser, activity: Option[String] = None, note: Option[String] = None, emailed: Option[Boolean] = None) = {
      user.credentials match {
        case cookies: AccessCredentials.Cookies =>
          setBehaviour(cookies, user.id, activity, note).onComplete {
            case Success(result) => Logger.info(s"Upserted ${user.id}")
            case Failure(err) => Logger.error(s"Failed to upsert membership-data-api behaviour for user ${user.id}", err)
          }
        case _ => Logger.error(s"Unexpected credentials for addBehaviour ($activity) for ${user.credentials}")
      }
    }

    def removeBehaviour(user: AuthenticatedIdUser, activity: Option[String] = None) = user.credentials match {
      case cookies: AccessCredentials.Cookies =>
        deleteBehaviour(cookies, user.id, activity).onComplete {
          case Success(result) => Logger.info(s"Cleared behaviours for ${user.user.id}")
          case Failure(err) => Logger.error(s"Failed to remove behaviour events via membership-data-api for user ${user.id}", err)
        }
      case _ => Logger.error(s"Unexpected credentials for removeBehaviour for ${user.credentials}")
    }

    private def getAttributes(cookies: AccessCredentials.Cookies) = AttributeHelper(cookies).get[Attributes]("user-attributes/me/membership")

    private def setBehaviour(cookies: AccessCredentials.Cookies, userId: String, activity: Option[String], note: Option[String]) = {
      val json: JsValue = Json.obj(
        "userId" -> userId,
        "activity" -> activity,
        "lastObserved" -> DateTime.now.toString(ISODateTimeFormat.dateTime.withZoneUTC),
        "note" -> note
      )
      BehaviourHelper(cookies).post[Behaviour]("user-behaviour/capture", json)
    }

    private def deleteBehaviour(cookies: AccessCredentials.Cookies, userId: String, activity: Option[String]) = {
      val json: JsValue = Json.obj(
        "userId" -> userId,
        "activity" -> activity
      )
      BehaviourHelper(cookies).post[Behaviour]("user-behaviour/remove", json)
    }
  }
}
