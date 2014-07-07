package model

import com.github.nscala_time.time.Imports._

case class Member(userId: String, tier: Tier.Tier, customerId: String, joinDate: Option[DateTime] = None)

object Member {
  def friend(userId: String, tier: Tier.Tier) = Member(userId, tier, " ") // DynamoDB doesn't accept empty strings
}
