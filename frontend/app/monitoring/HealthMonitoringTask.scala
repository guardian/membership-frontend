package monitoring

import java.io.File
import java.lang.management.ManagementFactory

import akka.actor.ActorSystem
import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.services.cloudwatch.model._
import com.amazonaws.util.EC2MetadataUtils
import com.gu.monitoring.CloudWatch.cloudwatch
import com.gu.monitoring.CloudWatchHealth
import com.typesafe.scalalogging.StrictLogging
import configuration.Config
import monitoring.MetricWriter.MetricItem
import monitoring.Scheduler.TaskDefinition

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}
import scala.concurrent.duration._
import scala.collection.JavaConverters._

object HealthMonitoringTask extends StrictLogging {

  def start(implicit system: ActorSystem, executionContext: ExecutionContext, stage: String): Unit = {

    val dimensions: Seq[Dimension] = Seq(
      new Dimension().withName("Stage").withValue(stage),
      new Dimension().withName("Services").withValue("jvm")
    ) ++
      Option(EC2MetadataUtils.getInstanceId).map { instanceId =>
        new Dimension().withName("InstanceId").withValue(instanceId)
      }.toList

    val metricDefinitions = Seq[MetricItem](
      MetricItem("max-heap-memory", ManagementFactory.getMemoryMXBean.getHeapMemoryUsage.getMax, StandardUnit.Bytes),
      MetricItem("used-heap-memory", ManagementFactory.getMemoryMXBean.getHeapMemoryUsage.getUsed, StandardUnit.Bytes),
      MetricItem("committed-heap-memory", ManagementFactory.getMemoryMXBean.getHeapMemoryUsage.getCommitted, StandardUnit.Bytes),
      MetricItem("max-non-heap-memory", ManagementFactory.getMemoryMXBean.getNonHeapMemoryUsage.getMax, StandardUnit.Bytes),
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

    Scheduler.startTask(
      task = TaskDefinition("HealthMonitoringTask", () => MetricWriter.putAll(dimensions, metricDefinitions)),
      initialDelay = 1.second,
      interval = 10.seconds
    )
  }

}

object Scheduler extends StrictLogging {

  case class TaskDefinition(name: String, task: () => Future[Unit])

  def startTask(
    task: TaskDefinition, initialDelay: FiniteDuration, interval: FiniteDuration
  )(implicit system: ActorSystem, executionContext: ExecutionContext) = {
    println("START")
    logger.info(s"Starting $task.name scheduled task with an initial delay of: $initialDelay. This task will refresh every: $interval")
    system.scheduler.schedule(initialDelay, interval) {
      task.task().onComplete {
        case Success(t) =>
          println("SUCCESS")
          logger.error(s"Scheduled task $task.name succeeded. This task will retry in: $interval")
        case Failure(e) =>
          println("FAIL")
          logger.error(s"Scheduled task $task.name failed due to: $e. This task will retry in: $interval")
      }
    }
  }

}

object MetricWriter {

  case class MetricItem(name: String, value: () => Long, unit: StandardUnit)

  def putAll(dimensions: Seq[Dimension], metricDefinitions: Seq[MetricItem]) = {
    Future.sequence(metricDefinitions.map { metric =>
      val datum =
        new MetricDatum()
          .withValue(metric.value().toDouble)
          .withMetricName(metric.name)
          .withUnit(metric.unit)
          .withDimensions(dimensions: _*)

      put(datum)
    }).map(_ => ())
  }

  def put(metric: MetricDatum): Future[Unit] = {

    println(s"pushing $metric")

    val request = new PutMetricDataRequest().
      withNamespace("membership").withMetricData(metric)

    val promise = Promise[Unit]()
    //    cloudwatch.putMetricDataAsync(request, new AsyncHandler[PutMetricDataRequest, PutMetricDataResult] {
    //
    //      def onError(exception: Exception): Unit = {
    //        logger.info(s"CloudWatch PutMetricDataRequest error: ${exception.getMessage}}")
    //        promise.failure(exception)
    //      }
    //
    //      def onSuccess(request: PutMetricDataRequest, result: PutMetricDataResult ): Unit = {
    //        logger.trace("CloudWatch PutMetricDataRequest - success")
    //        CloudWatchHealth.hasPushedMetricSuccessfully = true
    promise.success(())
    //      }
    //
    //    })
    promise.future
  }

}
