package monitoring

import com.gu.monitoring.StatusMetrics

object IdentityApiMetrics extends Metrics with StatusMetrics {
  val service = "Identity API"
}

object GridApiMetrics extends Metrics with StatusMetrics {
  val service = "Grid API"
}

object CASMetrics extends Metrics with StatusMetrics {
  val service = "CAS"
}