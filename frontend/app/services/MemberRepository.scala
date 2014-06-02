package services

import awscala.dynamodbv2.{ Item, DynamoDB }
import configuration.Config
import model.{ Tier, Member }

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