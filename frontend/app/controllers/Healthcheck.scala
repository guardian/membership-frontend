package controllers

import com.github.nscala_time.time.Imports._
import com.gu.monitoring.CloudWatchHealth
import com.gu.monitoring.SafeLogger.warn
import play.api.mvc.{Action, BaseController, ControllerComponents}
import services.{EventbriteCollectiveServices, GuardianLiveEventService, TouchpointBackend, TouchpointBackends}

trait Test {
  def ok: Boolean
  def messages: Seq[String] = Nil
}

class BoolTest(name: String, exec: () => Boolean) extends Test {
  override def messages = List(s"Test $name failed, health check will fail")
  override def ok = exec()
}

class Healthcheck(eventbriteService: EventbriteCollectiveServices, touchpointBackends: TouchpointBackends, override protected val controllerComponents: ControllerComponents) extends BaseController {

  def tests = Seq(
    //new BoolTest("Events", () => eventbriteService.guardianLiveEventService.events.nonEmpty),
    new BoolTest("CloudWatch", () => CloudWatchHealth.hasPushedMetricSuccessfully),
  )

  def healthcheck() = Action {
    Cached(1) {
      val failures = tests.filterNot(_.ok)
      if (failures.isEmpty) {
        Ok("OK")
      } else {
        failures.flatMap(_.messages).foreach(msg => warn(msg))
        ServiceUnavailable("Service Unavailable")
      }
    }
  }
}
