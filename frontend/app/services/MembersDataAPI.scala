package services

import com.gu.membership.salesforce.{FreeTierMember, Member, PaidTierMember, Tier}
import com.gu.membership.util.WebServiceHelper
import com.gu.monitoring.StatusMetrics
import com.squareup.okhttp.Request
import configuration.Config
import monitoring.MembersDataAPIMetrics
import play.api.Logger
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.functional.syntax._
import play.api.libs.json.Reads._
import play.api.libs.json._
import play.api.mvc.Cookie

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
    def fromMember(member: Member) = member match {
      case PaidTierMember(n, t) => Attributes(t, Some(n))
      case FreeTierMember(t) => Attributes(t, None)
    }
  }

  case class ApiError(message: String, details: String) extends RuntimeException(s"$message - $details")

  implicit val errorReads: Reads[ApiError] = (
    (JsPath \ "message").read[String] and
    (JsPath \ "details").read[String]
  )(ApiError)

  case class Helper(cookies: Seq[Cookie]) extends WebServiceHelper[Attributes, ApiError] {
    override val wsUrl: String = Config.membersDataAPIUrl
    override def wsPreExecute(req: Request.Builder): Request.Builder = req.addHeader(
      "Cookie", cookies.map(c => s"${c.name}=${c.value}").mkString("; "))
    override val wsMetrics: StatusMetrics = MembersDataAPIMetrics
  }

  object Service  {
    def check(member: Member)(cookies: Seq[Cookie]) = get(cookies).onComplete {
      case Success(attrs) =>
        if (attrs != Attributes.fromMember(member)) {
          Logger.warn(s"Members data API response doesn't match the expected member data. Expected ${Attributes.fromMember(member)}, got $attrs")
          MembersDataAPIMetrics.put("members-data-api-mismatch", 1)
        } else {
          Logger.debug("Members data API response matches the expected member data")
          MembersDataAPIMetrics.put("members-data-api-match", 1)
        }
      case Failure(err) => Logger.error(s"Failed while querying the members-data-api (OK in dev)", err)
    }

    private def get(cookies: Seq[Cookie]) = Helper(cookies).get[Attributes]("user-attributes/me/membership")
  }
}
