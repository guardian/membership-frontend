package monitoring

import com.gu.monitoring.StatusMetrics

object IdentityApiMetrics extends Metrics with StatusMetrics {
  val service = "Identity API"
}