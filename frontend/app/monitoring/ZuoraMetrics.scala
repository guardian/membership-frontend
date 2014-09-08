package monitoring

import com.gu.monitoring.{StatusMetrics, AuthenticationMetrics}

object ZuoraMetrics extends Metrics with StatusMetrics with AuthenticationMetrics {
  override val service = "Zuroa"

  def recordError {
    put("error-count", 1)
  }
}
