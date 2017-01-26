package monitoring

import com.amazonaws.regions.{Regions, Region}
import com.gu.monitoring.CloudWatch
import configuration.Config

trait Metrics extends CloudWatch {
  val stage = Config.stage
  val application = "membership" // This sets the namespace for Custom Metrics in AWS (see CloudWatch)
}
