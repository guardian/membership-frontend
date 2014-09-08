package monitoring

import com.gu.monitoring.StatusMetrics

object StripeMetrics extends Metrics with StatusMetrics {
  val service: String = "Stripe"
}
