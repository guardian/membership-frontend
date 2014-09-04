package monitoring

import com.gu.membership.salesforce.Tier

object MemberMetrics extends CloudWatch {

  def putSignUp(tier: Tier.Tier) {
    put(s"sign-ups-${tier.toString}")
  }

  def putUpgrade(tier: Tier.Tier) {
    put(s"upgrade-${tier.toString}")
  }

  def putDowngrade(tier:Tier.Tier) {
    put(s"downgrade-${tier.toString}")
  }

  def putCancel(tier:Tier.Tier) {
    put(s"cancel-${tier.toString}")
  }

  private def put(metricName: String) {
    put("Member", Map(metricName -> 1))
  }

}