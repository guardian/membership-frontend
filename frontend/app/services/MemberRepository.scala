package services

import awscala.dynamodbv2.DynamoDB

sealed trait MemberRepository {
  def putTier(userId: String, tier: String): Unit
  def getTier(userId: String): Option[String]
}

object AwsMemberTable extends MemberRepository {
  implicit val dynamoDB = DynamoDB("", "").at(awscala.Region.EU_WEST_1)
  val table = dynamoDB.table("members").get

  def putTier(key: String, tier: String): Unit = table.put(key, "tier" -> tier)

  def getTier(key: String): Option[String] = table.get(key).flatMap(_.attributes.find(_.name == "tier")).map(_.value.getS)
}

