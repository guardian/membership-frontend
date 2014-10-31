package model

import com.gu.membership.salesforce.Tier
import com.gu.membership.salesforce.Tier.Tier

case class PaidPlan(monthly: String, annual: String)

trait TierPlan {
  val tier: Tier
}

object FriendTierPlan extends TierPlan {
  val tier = Tier.Friend

  override val hashCode = 0 // This is here to give TouchpointBackendConfig a consistent hash over multiple JVM runs
}

case class PaidTierPlan(tier: Tier, annual: Boolean) extends TierPlan {
  assert(tier >= Tier.Partner)
}
