package services

import java.math.BigInteger
import scala.concurrent.Future
import scala.collection.JavaConverters._
import play.api.Logger

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.regions.{Regions, Region}
import com.amazonaws.services.dynamodbv2.model._

import model.{Tier, Member}
import model.Eventbrite.{EBEvent, EBDiscount}
import awscala.DateTime

trait MemberService {
  def put(member: Member): Unit

  def get(userId: String): Option[Member]
  def getByCustomerId(customerId: String): Option[Member]

  def createEventDiscount(userId: String, event: EBEvent): Option[Future[EBDiscount]]
}

object MemberService extends MemberService {

  val TABLE_NAME = "members"

  object Keys {
    val USER_ID = "userId"
    val TIER = "tier"
    val CUSTOMER_ID = "customerId"
    val JOIN_DATE = "joinDate"
  }

  val client = new AmazonDynamoDBClient
  client.setRegion(Region.getRegion(Regions.EU_WEST_1))

  private def att(value: String) = new AttributeValue(value)

  def put(member: Member): Unit = {
    Logger.debug(s"Putting member $member")

    client.putItem(TABLE_NAME, Map(
      Keys.USER_ID -> att(member.userId),
      Keys.TIER -> att(member.tier.toString),
      Keys.CUSTOMER_ID -> att(member.customerId),
      Keys.JOIN_DATE -> att("todo")
    ).asJava)
  }

  private def getMember(attrs: Map[String, AttributeValue]): Option[Member] = {
    for {
      id <- attrs.get(Keys.USER_ID)
      tier <- attrs.get(Keys.TIER)
      customerId <- attrs.get(Keys.CUSTOMER_ID)
    } yield Member(id.getS, Tier.withName(tier.getS), customerId.getS, DateTime.now) //todo this needs to be joinDate
  }

  def get(userId: String): Option[Member] = {
    for {
      attrsJ <- Option(client.getItem(TABLE_NAME, Map(Keys.USER_ID -> att(userId)).asJava).getItem)
      attrs = attrsJ.asScala.toMap
      member <- getMember(attrs)
    } yield member
  }

  def getByCustomerId(customerId: String): Option[Member] = {
    val cond = new Condition()
      .withComparisonOperator(ComparisonOperator.EQ)
      .withAttributeValueList(att(customerId))

    val query = new QueryRequest()
      .withTableName(TABLE_NAME)
      .withIndexName(s"${Keys.CUSTOMER_ID}-index")
      .withKeyConditions(Map(Keys.CUSTOMER_ID -> cond).asJava)

    for {
      attrsJ <- client.query(query).getItems.asScala.headOption
      attrs = attrsJ.asScala.toMap
      member <- getMember(attrs)
    } yield member
  }

  def createEventDiscount(userId: String, event: EBEvent): Option[Future[EBDiscount]] = {
    def encode(code: String) = {
      val md = java.security.MessageDigest.getInstance("SHA-1")
      val digest = md.digest(code.getBytes)
      new BigInteger(digest).abs.toString(36).toUpperCase.substring(0, 8)
    }

    for {
      member <- get(userId)
      if member.tier == Tier.Patron || member.tier == Tier.Partner
      // code should be unique for each user/event combination
      code = encode(s"${member.userId}_${event.id}")
    } yield {
      EventbriteService.createOrGetDiscount(event.id, code)
    }
  }
}