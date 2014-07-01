package model

case class Member(userId: String, tier: Tier.Tier, customerId: String)

object Member {
  def friend(userId: String, tier: Tier.Tier) = Member(userId, tier, " ") // DynamoDB doesn't accept empty strings
}
