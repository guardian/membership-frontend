package services

import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import akka.agent.Agent

import org.joda.time.DateTime

import play.api.libs.concurrent.Akka
import play.api.Logger
import play.api.http.Status.{OK, NOT_FOUND}
import play.api.Play.current
import play.api.libs.ws.WS
import play.api.libs.json.{JsValue, Json, JsPath, Reads}
import play.api.libs.functional.syntax._

import com.gu.scalaforce.Scalaforce

import configuration.Config
import model.{Tier, Member}
import model.Tier.Tier
import model.Eventbrite.{EBEvent, EBDiscount}
import model.Stripe.Subscription

case class MemberServiceError(s: String) extends Throwable {
  override def getMessage: String = s
}

abstract class MemberService {
  val salesforce: Scalaforce

  object Keys {
    val LAST_NAME = "LastName"
    val USER_ID = "IdentityID__c"
    val CUSTOMER_ID = "Stripe_Customer_ID__c"
    val TIER = "Membership_Tier__c"
    val OPT_IN = "Membership_Opt_In__c"
    val CREATED = "CreatedDate"
  }

  def contactURL(key: String, id: String): String = s"/services/data/v29.0/sobjects/Contact/$key/$id"

  implicit val readsDateTime = JsPath.read[String].map(s => new DateTime(s))
  implicit val readsMember: Reads[Member] = (
    (JsPath \ Keys.USER_ID).read[Int].map(_.toString) and
      (JsPath \ Keys.TIER).read[String].map(Tier.withName) and
      (JsPath \ Keys.CUSTOMER_ID).read[String] and
      (JsPath \ Keys.CREATED).read[DateTime] and
      (JsPath \ Keys.OPT_IN).read[Boolean]
    )(Member.apply _)

  def update(member: Member): Future[Member] = {
    for {
      result <- salesforce.patch(
        contactURL(Keys.USER_ID, member.userId),
        Json.obj(
          Keys.CUSTOMER_ID -> member.customerId,
          Keys.LAST_NAME-> "LAST NAME",
          Keys.TIER -> member.tier.toString,
          Keys.OPT_IN -> member.optedIn
        )
      )
    } yield member
  }

  def insert(userId: String, customerId: String, tier: Tier): Future[Option[Member]] = {
    for {
      result <- salesforce.patch(
        contactURL(Keys.USER_ID, userId),
        Json.obj(
          Keys.CUSTOMER_ID -> customerId,
          Keys.LAST_NAME -> "LAST NAME",
          Keys.TIER -> tier.toString
        )
      )
    } yield Some(result.json.as[Member])
  }

  private def getMember(key: String, id: String): Future[Option[Member]] = {
    for {
      result <- salesforce.get(contactURL(key, id))
    } yield {
      result.status match {
        case OK => Some(result.json.as[Member])
        case NOT_FOUND => None
        case code =>
          Logger.error(s"getMember failed, Salesforce returned $code")
          throw MemberServiceError(s"Salesforce returned $code")
      }
    }
  }

  def get(userId: String): Future[Option[Member]] = getMember(Keys.USER_ID, userId)
  def getByCustomerId(customerId: String): Future[Option[Member]] = getMember(Keys.CUSTOMER_ID, customerId)

  def delete(member: Member) = {
    // TODO: do we actually delete in SF?
  }

  def createEventDiscount(userId: String, event: EBEvent): Future[Option[EBDiscount]] = {

    def createDiscountFor(memberOpt: Option[Member]): Option[Future[EBDiscount]] = {
      // code should be unique for each user/event combination
      memberOpt
        .filter(_.tier >= Tier.Partner)
        .map { member =>
          EventbriteService.createOrGetDiscount(event.id, DiscountCode.generate(s"${member.userId}_${event.id}"))
        }
    }

    for {
      member <- get(userId)
      discount <- Future.sequence(createDiscountFor(member).toSeq)
    } yield discount.headOption
  }

  def cancelPayment(member:Member): Future[Option[Subscription]] = {
    for {
      customer <- StripeService.Customer.read(member.customerId)
      cancelledOpt = customer.subscription.map { subscription =>
        StripeService.Subscription.delete(customer.id, subscription.id)
      }
      cancelledSubscription <- Future.sequence(cancelledOpt.toSeq)
    } yield cancelledSubscription.headOption
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