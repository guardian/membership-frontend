package services

import java.math.BigInteger
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import org.joda.time.DateTimeZone.UTC
import com.github.nscala_time.time.Imports._
import scala.collection.JavaConverters._

import play.api.Logger

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.regions.{Regions, Region}
import com.amazonaws.services.dynamodbv2.model._

import model.{Tier, Member}
import model.Eventbrite.{EBEvent, EBDiscount}

trait MemberService {
  def put(member: Member): Future[Unit]

  def get(userId: String): Future[Option[Member]]
  def getByCustomerId(customerId: String): Future[Option[Member]]

  def createEventDiscount(userId: String, event: EBEvent): Future[Option[EBDiscount]]
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

  def put(member: Member): Future[Unit] = Future.successful {
    Logger.debug(s"Putting member $member")

    client.putItem(TABLE_NAME, Map(
      Keys.USER_ID -> att(member.userId),
      Keys.TIER -> att(member.tier.toString),
      Keys.CUSTOMER_ID -> att(member.customerId),
      Keys.JOIN_DATE -> att(member.joinDate.getOrElse(DateTime.now.toDateTime(UTC)).toString)
    ).asJava)
  }

  private def getMember(attrs: Map[String, AttributeValue]): Option[Member] = {
    for {
      id <- attrs.get(Keys.USER_ID)
      tier <- attrs.get(Keys.TIER)
      customerId <- attrs.get(Keys.CUSTOMER_ID)
      joinDate <- attrs.get(Keys.JOIN_DATE)
    } yield Member(id.getS, Tier.withName(tier.getS), customerId.getS, Some(new DateTime(joinDate.getS)))
  }

  def get(userId: String): Future[Option[Member]] = {
    val memberOpt = for {
      attrsJ <- Option(client.getItem(TABLE_NAME, Map(Keys.USER_ID -> att(userId)).asJava).getItem)
      attrs = attrsJ.asScala.toMap
      member <- getMember(attrs)
    } yield member

    Future.successful(memberOpt)
  }

  def getByCustomerId(customerId: String): Future[Option[Member]] = {
    val cond = new Condition()
      .withComparisonOperator(ComparisonOperator.EQ)
      .withAttributeValueList(att(customerId))

    val query = new QueryRequest()
      .withTableName(TABLE_NAME)
      .withIndexName(s"${Keys.CUSTOMER_ID}-index")
      .withKeyConditions(Map(Keys.CUSTOMER_ID -> cond).asJava)

    val memberOpt = for {
      attrsJ <- client.query(query).getItems.asScala.headOption
      attrs = attrsJ.asScala.toMap
      member <- getMember(attrs)
    } yield member

    Future.successful(memberOpt)
  }

  def createEventDiscount(userId: String, event: EBEvent): Future[Option[EBDiscount]] = {

    def createDiscountFor(memberOpt: Option[Member]): Option[Future[EBDiscount]] = {
      // code should be unique for each user/event combination
      memberOpt
        .filter { _.tier >= Tier.Partner }
        .map(_ => EventbriteService.createOrGetDiscount(event.id, DiscountCode.generate(s"${userId}_${event.id}")))
    }

    for {
      member <- get(userId)
      discount <- Future.sequence(createDiscountFor(member).toSeq)
    } yield discount.headOption
  }
}