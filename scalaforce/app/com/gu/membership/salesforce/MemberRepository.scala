package com.gu.membership.salesforce

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

import play.api.Logger
import play.api.http.Status.{OK, CREATED, NOT_FOUND}
import play.api.libs.json.Json

import com.gu.identity.model.User

import Member.Keys
import MemberDeserializer._
import Tier.Tier

case class MemberRepositoryError(s: String) extends Throwable {
  override def getMessage: String = s
}

abstract class MemberRepository {

  val salesforce: Scalaforce

  def contactURL(key: String, id: String): String = s"/services/data/v29.0/sobjects/Contact/$key/$id"

  def update(member: Member): Future[Member] = {
    for {
      result <- salesforce.patch(
        contactURL(Keys.USER_ID, member.identityId),
        Json.obj(
          Keys.CUSTOMER_ID -> member.stripeCustomerId,
          Keys.LAST_NAME-> "LAST NAME",
          Keys.TIER -> member.tier.toString,
          Keys.OPT_IN -> member.optedIn
        )
      )
    } yield member
  }

  def insert(user: User, customerId: String, tier: Tier): Future[String] = {
    for {
      result <- salesforce.patch(
        contactURL(Keys.USER_ID, user.id),
        Json.obj(
          Keys.CUSTOMER_ID -> customerId,
          Keys.LAST_NAME -> "LAST NAME", // TODO: fill surname
          Keys.TIER -> tier.toString,
          Keys.EMAIL -> user.getPrimaryEmailAddress
        )
      )
    } yield {
      result.status match {
        case CREATED => (result.json \ "id").as[String]
        case code =>
          Logger.error(s"insert failed, Salesforce returned $code")
          throw MemberRepositoryError(s"Salesforce return $code")
      }
    }
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
          throw MemberRepositoryError(s"Salesforce returned $code")
      }
    }
  }

  def get(userId: String): Future[Option[Member]] = getMember(Keys.USER_ID, userId)
  def getByCustomerId(customerId: String): Future[Option[Member]] = getMember(Keys.CUSTOMER_ID, customerId)
}
