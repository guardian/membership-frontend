package monitoring

import com.gu.monitoring.StatusMetrics

object ContentApiMetrics extends Metrics with StatusMetrics {
  val service = "Content API"
}
