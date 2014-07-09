package services

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.Logger
import play.api.http.Status.{OK, NOT_FOUND}
import play.api.libs.json.{Json, JsPath, Reads}
import play.api.libs.functional.syntax._

import com.gu.scalaforce.Scalaforce

import model.{Tier, Member}
import model.Eventbrite.{EBEvent, EBDiscount}
import configuration.Config

case class MemberNotFound(userId: String) extends Throwable {
  override def getMessage: String = s"Member with ID $userId not found"
}

trait MemberService {
  def put(member: Member): Future[Member]

  def get(userId: String): Future[Member]
  def getByCustomerId(customerId: String): Future[Member]

  def createEventDiscount(userId: String, event: EBEvent): Future[Option[EBDiscount]]
}

object MemberService extends MemberService with Scalaforce {
  val consumerKey = Config.salesforceConsumerKey
  val consumerSecret = Config.salesforceConsumerSecret

  val apiURL = Config.salesforceApiUrl
  val apiUsername = Config.salesforceApiUsername
  val apiPassword = Config.salesforceApiPassword
  val apiToken = Config.salesforceApiToken

  val contactURL = "/services/data/v29.0/sobjects/Contact"
  def contactURL(key: String, id: String): String = s"$contactURL/$key/$id"

  object Keys {
    val LAST_NAME = "LastName"
    val USER_ID = "IdentityID__c"
    val CUSTOMER_ID = "Stripe_Customer_ID__c"
    val TIER = "Membership_Tier__c"
  }

  implicit val readsMember: Reads[Member] = (
    (JsPath \ Keys.USER_ID).read[Int].map(_.toString) and
      (JsPath \ Keys.TIER).read[String].map(Tier.withName) and
      (JsPath \ Keys.CUSTOMER_ID).read[String]
    )((userId, tier, customerId) => Member(userId, tier, customerId, None)
    )

  def put(member: Member): Future[Member] = {
    for {
      result <- request(contactURL(Keys.USER_ID, member.userId)).patch(
        Json.obj(
          Keys.CUSTOMER_ID -> member.customerId,
          Keys.LAST_NAME-> "LAST NAME",
          Keys.TIER -> member.tier.toString
        )
      )
    } yield member
  }

  private def getMember(key: String, id: String): Future[Member] = {
    for {
      result <- request(contactURL(key, id)).get()
    } yield {
      result.status match {
        case OK => result.json.as[Member]
        case NOT_FOUND => throw MemberNotFound(id)
        case code =>
          Logger.error(s"getMember failed, Salesforce returned $code")
          throw new Exception("blah")
      }
    }
  }

  def get(userId: String): Future[Member] = getMember(Keys.USER_ID, userId)
  def getByCustomerId(customerId: String): Future[Member] = getMember(Keys.CUSTOMER_ID, customerId)

  def createEventDiscount(userId: String, event: EBEvent): Future[Option[EBDiscount]] = {

    def createDiscountFor(member: Member): Future[Option[EBDiscount]] = {
      // code should be unique for each user/event combination
      member.tier match {
        case Tier.Partner | Tier.Patron =>
          EventbriteService.createOrGetDiscount(event.id, DiscountCode.generate(s"${userId}_${event.id}")).map(Some(_))
        case _ => Future.successful(None)
      }
    }

    for {
      member <- get(userId)
      discount <- createDiscountFor(member)
    } yield discount
  }
}
