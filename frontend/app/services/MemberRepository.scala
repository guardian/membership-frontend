package services

import awscala.dynamodbv2.{ Item, DynamoDB }
import configuration.Config
import model.{ Tier, Member }
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.regions.{Regions, Region}
import com.amazonaws.services.dynamodbv2.model.{AttributeValue, GetItemRequest, PutItemRequest}
import scala.collection.JavaConverters._

sealed trait MemberRepository {
  def put(member: Member): Unit
  def get(userId: String): Option[Member]
}

object AwsMemberTable extends MemberRepository {
  implicit val dynamoDB = DynamoDB(Config.awsAccessKey, Config.awsSecretKey).at(awscala.Region.EU_WEST_1)
  val table = dynamoDB.table("members").get

  private def getAttribute(item: Item, key: String) = item.attributes.find(_.name == key).map(_.value.getS)

  def put(member: Member): Unit =
    table.put(member.userId, "tier" -> member.tier.toString, "customerId" -> member.customerId)

  def get(userId: String): Option[Member] = for {
    member <- table.get(userId)
    tier <- getAttribute(member, "tier")
    customerId <- getAttribute(member, "customerId")
  } yield Member(userId, Tier.withName(tier), customerId)
}


object JavaAwsMemberTable extends MemberRepository {

  val client = new AmazonDynamoDBClient
  client.setRegion(Region.getRegion(Regions.EU_WEST_1))

  def put(member: Member): Unit = {
    val key = Map("id" -> att(member.userId), "tier" -> att(member.tier.toString), "customerId" -> att(member.customerId)).asJava
    client.putItem(new PutItemRequest("members", key))
  }

  def get(userId: String): Option[Member] = {
    val key = Map("id" -> att(userId)).asJava
    val itemRequest = new GetItemRequest("members", key)
    val item = Option(client.getItem(itemRequest).getItem)
    item.map { result =>
      val (id, tier, customer) = (result.get("id").getS, result.get("tier").getS, result.get("customerId").getS)
      Member(id, Tier.withName(tier), customer)
    }
  }

  def att(value: String) = new AttributeValue(value)
}