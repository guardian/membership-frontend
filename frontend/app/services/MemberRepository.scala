package services

import awscala.dynamodbv2.DynamoDB
import configuration.Config
import model.Tier
import model.Tier.Tier

sealed trait MemberRepository {
  def putTier(userId: String, tier: Tier): Unit
  def getTier(userId: String): Tier
}

object AwsMemberTable extends MemberRepository {
  implicit val dynamoDB = DynamoDB(Config.awsAccessKey, Config.awsSecretKey).at(awscala.Region.EU_WEST_1)
  val table = dynamoDB.table("members").get

  def putTier(key: String, tier: Tier): Unit = table.put(key, "tier" -> tier.toString)

  def getTier(key: String): Tier = table.get(key)
    .flatMap(_.attributes.find(_.name == "tier"))
    .map(_.value.getS)
    .fold(Tier.RegisteredUser)(Tier.withName)
}

