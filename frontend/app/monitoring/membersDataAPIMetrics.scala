package monitoring

import com.gu.monitoring.StatusMetrics

object MembersDataAPIMetrics extends Metrics with StatusMetrics {
  override val service: String = "Members Data API"
}
