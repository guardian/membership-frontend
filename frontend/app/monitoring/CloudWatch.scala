package monitoring

import java.util.concurrent.Future

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.regions.{Region, Regions, ServiceAbbreviations}
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClient
import com.amazonaws.services.cloudwatch.model.{Dimension, MetricDatum, PutMetricDataRequest}
import configuration.Config
import play.api.Logger


trait CloudWatch {
  private lazy val region = Region.getRegion(Regions.EU_WEST_1)
  private lazy val stageDimension = new Dimension().withName("Stage").withValue(Config.stage)


  lazy val cloudwatch = {
    val client = new AmazonCloudWatchAsyncClient(new DefaultAWSCredentialsProviderChain)
    client.setEndpoint(region.getServiceEndpoint(ServiceAbbreviations.CloudWatch))
    client
  }

  trait LoggingAsyncHandler extends AsyncHandler[PutMetricDataRequest, Void]
  {
    def onError(exception: Exception)
    {
      Logger.info(s"CloudWatch PutMetricDataRequest error: ${exception.getMessage}}")
    }
    def onSuccess(request: PutMetricDataRequest, result: Void )
    {
      Logger.info("CloudWatch PutMetricDataRequest - success")
    }
  }

  object LoggingAsyncHandler extends LoggingAsyncHandler


  def put(namespace: String, metrics: Map[String, Double], dimensions: Seq[Dimension]): Future[Void] = {
    val metricsData = metrics.map{ case (name, count) =>
      new MetricDatum()
        .withValue(count)
        .withMetricName(name)
        .withUnit("Count")
        .withDimensions(dimensions: _*)
    }.toSeq

    val request = new PutMetricDataRequest().
      withNamespace(namespace).withMetricData(metricsData: _*)

    cloudwatch.putMetricDataAsync(request, LoggingAsyncHandler)
  }

  def put(namespace: String, metrics: Map[String, Double]): Unit =
    put(namespace, metrics, Seq(stageDimension))
}

object IdentityApiMetrics extends CloudWatch {

  def putPasswordsExistsResponse(status: Int) {
    putStatus("identity-get-user-password-exists-response", status)
  }

  def putUserDetailsResponse(status: Int) {
    putStatus("identity-get-user-details-response", status)
  }

  def putUpdateUserDetailsResponse(status : Int) {
    putStatus("identity-update-user-details-response", status)
  }

  def putPasswordUpdateResponse(status: Int) {
    putStatus("identity-password-update-response", status)
  }

  private def putStatus(name: String, status: Int) {
    val statusClass = status / 100
    val metrics = Map(s"$name-${statusClass}XX" -> 1.00)
    put("Identity API", metrics)
  }
}