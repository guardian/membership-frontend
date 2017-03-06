package monitoring

import com.amazonaws.services.cloudwatch.model.Dimension
import configuration.Config

trait TouchpointBackendMetrics extends Metrics {
  val backendEnv: String
  override def mandatoryDimensions = super.mandatoryDimensions :+ new Dimension().withName("Backend").withValue(backendEnv)
}
