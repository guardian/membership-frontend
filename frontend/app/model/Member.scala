package model

import org.joda.time.DateTime

case class Member(userId: String, tier: Tier.Tier, customerId: String, joinDate: DateTime)

object Member {
  def friend(userId: String, tier: Tier.Tier, joinDate: DateTime) = Member(userId, tier, " ", joinDate) // DynamoDB doesn't accept empty strings
}
