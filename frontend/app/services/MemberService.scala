package services

import model.{Tier, Member}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.regions.{Regions, Region}
import com.amazonaws.services.dynamodbv2.model.AttributeValue
import scala.collection.JavaConverters._

trait MemberService {
  def put(member: Member): Unit

  def get(userId: String): Option[Member]
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
}