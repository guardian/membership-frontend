package monitoring

import com.gu.membership.salesforce.Tier
import model.ProductRatePlan

class MemberMetrics(val backendEnv: String) extends TouchpointBackendMetrics {

  val service = "Member"

  def putSignUp(plan: ProductRatePlan) {
    put(s"sign-ups-${plan.salesforceTier}")
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
    put(metricName, 1)
  }
}
