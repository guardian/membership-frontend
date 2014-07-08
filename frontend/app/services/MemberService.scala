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
import model.Stripe.Subscription

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
    val CANCELLATION_REQUESTED = "cancellationRequested"
  }

  val client = new AmazonDynamoDBClient
  client.setRegion(Region.getRegion(Regions.EU_WEST_1))

  private def att(value: String) = new AttributeValue(value)

  def put(member: Member): Unit = {
    Logger.debug(s"Putting member $member")

    val attrs = Map(
      Keys.USER_ID -> att(member.userId),
      Keys.TIER -> att(member.tier.toString),
      Keys.CUSTOMER_ID -> att(member.customerId),
      Keys.JOIN_DATE -> att(member.joinDate.getOrElse(DateTime.now.toDateTime(UTC)).toString)
    ) ++ (if (member.cancellationRequested) Some(Keys.CANCELLATION_REQUESTED -> att("true")) else None)

    client.putItem(TABLE_NAME, attrs.asJava)
  }

  private def getMember(attrs: Map[String, AttributeValue]): Option[Member] = {
    for {
      id <- attrs.get(Keys.USER_ID)
      tier <- attrs.get(Keys.TIER)
      customerId <- attrs.get(Keys.CUSTOMER_ID)
      joinDate <- attrs.get(Keys.JOIN_DATE)
    } yield {
      val cancellationRequested = attrs.get(Keys.CANCELLATION_REQUESTED).exists(_ => true)
      Member(id.getS, Tier.withName(tier.getS), customerId.getS, Some(new DateTime(joinDate.getS)), cancellationRequested)
    }
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

  def delete(member: Member) = {
    client.deleteItem(TABLE_NAME, Map(Keys.USER_ID -> att(member.userId)).asJava)
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