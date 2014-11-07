package monitoring

import com.gu.monitoring.StatusMetrics

object EventbriteMetrics extends Metrics with StatusMetrics {
  val service = "Eventbrite"

  def putThankyou(eventId: String) {
    put("user-returned-to-thankyou-page", 1)
  }
}