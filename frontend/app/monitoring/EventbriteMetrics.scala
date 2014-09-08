package monitoring

import com.gu.monitoring.StatusMetrics

object EventbriteMetrics extends Metrics with StatusMetrics {
  val namespace = "Eventbrite"

  def recordResponse(responseType: String, url: String, status:Int) {
    val name = s"$responseType-${url.replace("/", "-")}"
    putResponseCode(namespace, name, status)
  }
}