package monitoring

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.{Region, Regions, ServiceAbbreviations}
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClient
import com.amazonaws.services.cloudwatch.model.{Dimension, MetricDatum, PutMetricDataRequest}
import configuration.Config


trait CloudWatch {
  private lazy val region = Region.getRegion(Regions.EU_WEST_1)
  private lazy val stageDimension = new Dimension().withName("Stage").withValue(Config.stage)


  lazy val cloudwatch = {
    val client = new AmazonCloudWatchAsyncClient(new DefaultAWSCredentialsProviderChain)
    client.setEndpoint(region.getServiceEndpoint(ServiceAbbreviations.CloudWatch))
    client
  }

  def put(namespace: String, metrics: Map[String, Double], dimensions: Seq[Dimension]): Any = {
    val metricsData = metrics.map{ case (name, count) =>
      new MetricDatum()
        .withValue(count)
        .withMetricName(name)
        .withUnit("Count")
        .withDimensions(dimensions: _*)
    }.toSeq

    val request = new PutMetricDataRequest().
      withNamespace(namespace).withMetricData(metricsData: _*)

    cloudwatch.putMetricDataAsync(request)
  }

  def put(namespace: String, metrics: Map[String, Double]): Unit =
    put(namespace, metrics, Seq(stageDimension))
}

object IdentityApiCloudWatch extends CloudWatch {
  def put(metrics:Map[String, Double]): Unit = {
    put("Identity API", metrics)
  }
}