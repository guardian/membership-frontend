package monitoring

import java.io.File
import java.lang.management.ManagementFactory

import akka.actor.ActorSystem
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.cloudwatch.model._
import com.amazonaws.util.EC2MetadataUtils
import com.gu.monitoring.CloudWatch.cloudwatch
import com.gu.monitoring.CloudWatchHealth
import com.gu.monitoring.SafeLogger
import com.gu.monitoring.SafeLogger._
import monitoring.CloudwatchMetric.MetricItem
import monitoring.Scheduler.TaskDefinition
import scala.collection.JavaConverters._
import scala.concurrent.duration.{FiniteDuration, _}
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

object HealthMonitoringTask {

  def start(implicit system: ActorSystem, executionContext: ExecutionContext, stage: String, appName: String): Unit = {

    val dimensions: Seq[Dimension] = Seq(
      new Dimension().withName("App").withValue(appName),
      new Dimension().withName("Stage").withValue(stage),
      new Dimension().withName("Services").withValue("jvm")
    ) ++
      Option(EC2MetadataUtils.getInstanceId).map { instanceId =>
        new Dimension().withName("InstanceId").withValue(instanceId)
      }.toList

    val metricDefinitions = Seq[MetricItem](
      MetricItem("used-heap-memory", ManagementFactory.getMemoryMXBean.getHeapMemoryUsage.getUsed, StandardUnit.Bytes),
      MetricItem("committed-heap-memory", ManagementFactory.getMemoryMXBean.getHeapMemoryUsage.getCommitted, StandardUnit.Bytes),
      MetricItem("used-non-heap-memory", ManagementFactory.getMemoryMXBean.getNonHeapMemoryUsage.getUsed, StandardUnit.Bytes),
      MetricItem("committed-non-heap-memory", ManagementFactory.getMemoryMXBean.getNonHeapMemoryUsage.getCommitted, StandardUnit.Bytes),

      MetricItem("free-disk-space", new File("/").getUsableSpace, StandardUnit.Bytes),

      MetricItem("thread-count", ManagementFactory.getThreadMXBean.getThreadCount, StandardUnit.Count)
    ) ++
      ManagementFactory.getGarbageCollectorMXBeans.asScala.flatMap { gcBean =>
        val name = gcBean.getName.replace(" ", "_")
        Seq(
          MetricItem(s"$name-gc-count-per-min", gcBean.getCollectionCount, StandardUnit.Count),
          MetricItem(s"$name-gc-time-per-min", gcBean.getCollectionTime, StandardUnit.Milliseconds)
        )
      }

    Scheduler.schedule(
      task = TaskDefinition(
        name = "HealthMonitoringTask",
        task = () => CloudwatchMetric.putMetrics(dimensions, metricDefinitions)),
      initialDelay = 1.second,
      interval = 10.seconds
    )
  }

}

// simple stateless fire and forget scheduler
object Scheduler {

  case class TaskDefinition(name: String, task: () => Future[Unit])

  def schedule(
    task: TaskDefinition, initialDelay: FiniteDuration, interval: FiniteDuration
  )(implicit system: ActorSystem, executionContext: ExecutionContext) = {
    SafeLogger.info(s"Starting $task.name scheduled task with an initial delay of: $initialDelay. This task will refresh every: $interval")
    system.scheduler.schedule(initialDelay, interval) {
      task.task().onComplete {
        case Success(t) =>
          SafeLogger.debug(s"Scheduled task $task.name succeeded. This task will repeat in: $interval")
        case Failure(e) =>
          SafeLogger.error(scrub"Scheduled task $task.name failed due to: $e. This task will retry in: $interval")
      }
    }
  }

}

// simple stateless writer for cloudwatch metrics
object CloudwatchMetric {

  case class MetricItem(name: String, value: () => Long, unit: StandardUnit)

  def putMetrics(dimensions: Seq[Dimension], metricDefinitions: Seq[MetricItem])(implicit executionContext: ExecutionContext) = {
    Future.sequence(metricDefinitions.map { metric =>

      val datum =
        new MetricDatum()
          .withValue(metric.value().toDouble)
          .withMetricName(metric.name)
          .withUnit(metric.unit)
          .withDimensions(dimensions: _*)

      val request = new PutMetricDataRequest().
        withNamespace("membership").withMetricData(datum)

      val promise = Promise[Unit]()
      cloudwatch.putMetricDataAsync(request, new AsyncHandler[PutMetricDataRequest, PutMetricDataResult] {

        def onError(exception: Exception): Unit = {
          SafeLogger.info(s"CloudWatch PutMetricDataRequest error: ${exception.getMessage}}")
          promise.failure(exception)
        }

        def onSuccess(request: PutMetricDataRequest, result: PutMetricDataResult ): Unit = {
          SafeLogger.debug("CloudWatch PutMetricDataRequest - success")
          CloudWatchHealth.hasPushedMetricSuccessfully = true
          promise.success(())
        }

      })
      promise.future

    }).map(_ => ())
  }

}
