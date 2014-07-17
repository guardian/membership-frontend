package services

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.agent.Agent

import play.api.libs.concurrent.Akka
import play.api.Logger
import play.api.http.Status.{OK, NOT_FOUND}
import play.api.Play.current
import play.api.libs.ws.WS
import play.api.libs.json.{JsValue, Json, JsPath, Reads}
import play.api.libs.functional.syntax._

import com.gu.scalaforce.Scalaforce

import model.{Tier, Member}
import model.Eventbrite.{EBEvent, EBDiscount}
import configuration.Config

case class MemberNotFound(userId: String) extends Throwable {
  override def getMessage: String = s"Member with ID $userId not found"
}

abstract class MemberService {
  val salesforce: Scalaforce

  object Keys {
    val LAST_NAME = "LastName"
    val USER_ID = "IdentityID__c"
    val CUSTOMER_ID = "Stripe_Customer_ID__c"
    val TIER = "Membership_Tier__c"
  }

  def contactURL(key: String, id: String): String = s"/services/data/v29.0/sobjects/Contact/$key/$id"

  implicit val readsMember: Reads[Member] = (
    (JsPath \ Keys.USER_ID).read[Int].map(_.toString) and
      (JsPath \ Keys.TIER).read[String].map(Tier.withName) and
      (JsPath \ Keys.CUSTOMER_ID).read[String]
    )((userId, tier, customerId) => Member(userId, tier, customerId, None))

  def put(member: Member): Future[Member] = {
    for {
      result <- salesforce.patch(
        contactURL(Keys.USER_ID, member.userId),
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
      result <- salesforce.get(contactURL(key, id))
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

object MemberService extends MemberService {
  val accessToken = Agent[String]("")

  val salesforce = new Scalaforce {
    val consumerKey: String = Config.salesforceConsumerKey
    val consumerSecret: String = Config.salesforceConsumerSecret

    val apiURL: String = Config.salesforceApiUrl
    val apiUsername: String = Config.salesforceApiUsername
    val apiPassword: String = Config.salesforceApiPassword
    val apiToken: String = Config.salesforceApiToken

    def login(endpoint: String, params: Seq[(String, String)]) =
      WS.url(apiURL + endpoint).withQueryString(params: _*).post("")

    def get(endpoint: String) =
      WS.url(apiURL + endpoint).withHeaders("Authoriation" -> s"Bearer ${accessToken.get()}").get()

    def patch(endpoint: String, body: JsValue) =
      WS.url(apiURL + endpoint).withHeaders("Authorization" -> s"Bearer ${accessToken.get()}").patch(body)
  }

  private implicit val system = Akka.system

  def refresh() {
    Logger.debug("Refreshing Scalaforce token")
    accessToken.sendOff(_ => {
      val token = Await.result(salesforce.getAccessToken, 15.seconds)
      Logger.debug(s"Got token $token")
      token
    })
  }

  def start() {
    Logger.info("Starting Scalaforce background tasks")
    system.scheduler.schedule(0.seconds, 2.hours) { refresh() }
  }
}