package monitoring

import com.gu.salesforce.Tier

class MemberMetrics(val backendEnv: String) extends TouchpointBackendMetrics {

  val service = "Member"

  def putSignUp(tier: Tier) {
    put(s"sign-ups-${tier.name}")
  }

  def putUpgrade(tier: Tier) {
    put(s"upgrade-${tier.name}")
  }

  def putDowngrade(tier: Tier) {
    put(s"downgrade-${tier.name}")
  }

  def putCancel(tier: Tier) {
    put(s"cancel-${tier.name}")
  }

  def putFailSignUp(tier: Tier) {
    put(s"failed-sign-up-${tier.name}")
  }

  private def put(metricName: String) {
    put(metricName, 1)
  }
}
