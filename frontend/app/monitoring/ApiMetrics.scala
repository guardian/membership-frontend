package monitoring

import com.gu.monitoring.{ServiceMetrics, StatusMetrics}

object DummyMetrics extends ServiceMetrics("", "", "") with Metrics with StatusMetrics {
  override val service = "Dummy Service"

  override val stage = ""

  override val application = ""

  override def putResponseCode(status: Int, responseMethod: String): Unit = {}

  override def recordRequest() { }

  override def recordResponse(status: Int, responseMethod: String) { }

  override def recordAuthenticationError() { }

  override def recordError() {}
}
