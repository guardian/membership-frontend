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

case class MemberNotFound(userId: String) extends Throwable {
  override def getMessage: String = s"Member with ID $userId not found"
}

trait MemberService {
  def put(member: Member): Future[Unit]

  def get(userId: String): Future[Member]
  def getByCustomerId(customerId: String): Future[Member]

  def createEventDiscount(userId: String, event: EBEvent): Future[Option[EBDiscount]]
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

  def put(member: Member): Future[Unit] = Future.successful {
    Logger.debug(s"Putting member $member")

    val attrs = Map(
      Keys.USER_ID -> att(member.userId),
      Keys.TIER -> att(member.tier.toString),
      Keys.CUSTOMER_ID -> att(member.customerId),
      Keys.JOIN_DATE -> att(member.joinDate.getOrElse(DateTime.now.toDateTime(UTC)).toString)
    ) ++ (if (member.cancellationRequested) Some(Keys.CANCELLATION_REQUESTED -> att("true")) else None)

    client.putItem(TABLE_NAME, attrs.asJava)
  }

  private def getMember(attrs: Map[String, AttributeValue]): Option[Future[Member]] = {
    for {
      id <- attrs.get(Keys.USER_ID)
      tier <- attrs.get(Keys.TIER)
      customerId <- attrs.get(Keys.CUSTOMER_ID)
      joinDate <- attrs.get(Keys.JOIN_DATE)
    } yield {
      val cancellationRequested = attrs.get(Keys.CANCELLATION_REQUESTED).exists(_ => true)
      Future.successful(Member(id.getS, Tier.withName(tier.getS), customerId.getS, Some(new DateTime(joinDate.getS)), cancellationRequested))
    }
  }

  def get(userId: String): Future[Member] = {
    val memberOpt = for {
      attrsJ <- Option(client.getItem(TABLE_NAME, Map(Keys.USER_ID -> att(userId)).asJava).getItem)
      attrs = attrsJ.asScala.toMap
      member <- getMember(attrs)
    } yield member

    memberOpt.getOrElse(Future.failed(MemberNotFound(userId)))
  }

  def getByCustomerId(customerId: String): Future[Member] = {
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

    memberOpt.getOrElse(Future.failed(MemberNotFound(customerId)))
  }

  def delete(member: Member) = {
    client.deleteItem(TABLE_NAME, Map(Keys.USER_ID -> att(member.userId)).asJava)
  }

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