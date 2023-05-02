package com.gu.salesforce

import akka.actor.Scheduler
import com.gu.okhttp.RequestRunners
import okhttp3.{Request, Response}

import scala.concurrent.{ExecutionContext, Future}

class SimpleContactRepository(
    salesforceConfig: SalesforceConfig,
    scheduler: Scheduler,
    appName: String)(implicit executionContext: ExecutionContext)
  extends ContactRepository {

  val salesforce: Scalaforce = new Scalaforce {
    val application: String = appName
    val stage: String = salesforceConfig.envName
    val sfConfig: SalesforceConfig = salesforceConfig
    val httpClient: (Request) => Future[Response] = RequestRunners.futureRunner
    val sfScheduler = scheduler
  }
  salesforce.startAuth()
}
