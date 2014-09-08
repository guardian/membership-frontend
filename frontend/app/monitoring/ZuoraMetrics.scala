package monitoring

import com.gu.monitoring.{StatusMetrics, AuthenticationMetrics}

object ZuoraMetrics extends Metrics with StatusMetrics with AuthenticationMetrics {
  val service = "Zuora"

  def recordError {
    put("error-count", 1)
  }
}
