package model

case class Member(userId: String, tier: Tier.Tier, customerId: String)

object Member {
  val NO_CUSTOMER_ID = " "
  def friend(userId: String, tier: Tier.Tier) = Member(userId, tier, NO_CUSTOMER_ID) // DynamoDB doesn't accept empty strings
}
