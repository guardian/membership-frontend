package monitoring

import com.gu.monitoring.StatusMetrics
import com.amazonaws.services.cloudwatch.model.Dimension

class EventbriteMetrics(eventSource: String)
    extends Metrics with StatusMetrics {
  val service = "Eventbrite"

  override def mandatoryDimensions =
    super.mandatoryDimensions :+ new Dimension()
      .withName("Event Source")
      .withValue(eventSource)
}
