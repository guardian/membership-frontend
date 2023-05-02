package com.gu.monitoring

import java.util.concurrent.{CompletableFuture, Future}
import com.amazonaws.services.cloudwatch.model.{Dimension, PutMetricDataResult}

class ZuoraMetrics(val stage: String, val application: String, val service: String = "Zuora")
  extends CloudWatch
    with StatusMetrics
    with RequestMetrics
    with AuthenticationMetrics {

  def countRequest(): Unit = putRequest // just a nicer name
}


// Dummy for tests, and as default argument so API clients do not have to change
object NoOpZuoraMetrics extends ZuoraMetrics("dummy", "dummy") {
  override def put(name : String, count: Double, extraDimensions: Dimension*): Future[PutMetricDataResult] =
    CompletableFuture.completedFuture(null.asInstanceOf[PutMetricDataResult])
}