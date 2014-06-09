package services

import java.math.BigInteger
import scala.concurrent.Future
import scala.collection.JavaConverters._

import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.regions.{Regions, Region}
import com.amazonaws.services.dynamodbv2.model.AttributeValue

import model.{Tier, Member}
import model.Eventbrite.{EBEvent, EBDiscount}

trait MemberService {
  def put(member: Member): Unit

  def get(userId: String): Option[Member]

  def createEventDiscount(userId: String, event: EBEvent): Option[Future[EBDiscount]]
}

object MemberService extends MemberService {

  val client = new AmazonDynamoDBClient
  client.setRegion(Region.getRegion(Regions.EU_WEST_1))

  def put(member: Member): Unit = {
    val attributes = itemKey(member.userId) ++  Map("tier" -> att(member.tier.toString), "customerId" -> att(member.customerId))
    client.putItem("members", attributes.asJava)
  }

  def get(userId: String): Option[Member] = {
    val attributesOpt = Option(client.getItem("members", itemKey(userId).asJava).getItem)
    attributesOpt.map { attributes =>
      val (id, tier, customer) = (attributes.get("id").getS, attributes.get("tier").getS, attributes.get("customerId").getS)
      Member(id, Tier.withName(tier), customer)
    }
  }

  def itemKey(userId: String) = Map("id" -> att(userId))

  def att(value: String) = new AttributeValue(value)

  def createEventDiscount(userId: String, event: EBEvent): Option[Future[EBDiscount]] = {
    def encode(code: String) = {
      val md = java.security.MessageDigest.getInstance("SHA-1")
      val digest = md.digest(code.getBytes)
      new BigInteger(digest).toString(36).toUpperCase.substring(0, 8)
    }

    for {
      member <- get(userId)
      if member.tier == Tier.Patron || member.tier == Tier.Partner
      // code should be unique for each user/event combination
      code = encode(s"${member.userId}${event.id}")
    } yield EventbriteService.createDiscount(event.id, code)
  }
}