package model

import com.gu.membership.salesforce.Tier
import com.gu.membership.salesforce.Tier.Tier

object Subscription {
  case class Subscription(id: String)

  case class Amendment(ids: Seq[String])

  case class PaidPlan(monthly: String, annual: String)

  trait TierPlan {
    val tier: Tier
  }

  object FriendTierPlan extends TierPlan {
    val tier = Tier.Friend
  }

  case class PaidTierPlan(tier: Tier, annual: Boolean) extends TierPlan {
    assert(tier >= Tier.Partner)
  }
}
