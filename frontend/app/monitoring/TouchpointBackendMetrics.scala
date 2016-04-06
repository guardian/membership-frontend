package monitoring

import com.amazonaws.services.cloudwatch.model.Dimension
import configuration.Config

trait TouchpointBackendMetrics extends Metrics {
  val backendEnv: String

  val standardBackend = Config.stage == backendEnv

  override def mandatoryDimensions =
    if (standardBackend) super.mandatoryDimensions
    else {
      super.mandatoryDimensions :+ new Dimension()
        .withName("Backend")
        .withValue(backendEnv)
    }
}
