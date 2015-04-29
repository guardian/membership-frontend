package monitoring

import com.gu.membership.model.ProductRatePlan
import com.gu.membership.salesforce.Tier

class MemberMetrics(val backendEnv: String) extends TouchpointBackendMetrics {

  val service = "Member"

  def putSignUp(plan: ProductRatePlan) {
    put(s"sign-ups-${plan.salesforceTier}")
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

  def putFailSignUp(plan: ProductRatePlan) {
    put(s"failed-sign-up-${plan.salesforceTier}")
  }

  private def put(metricName: String) {
    put(metricName, 1)
  }
}
