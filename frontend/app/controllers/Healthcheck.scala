
package controllers

import com.github.nscala_time.time.Imports._
import com.gu.membership.salesforce.Tier
import com.gu.monitoring.CloudWatchHealth
import model.{TierPricing, Pricing}
import play.api.Logger.warn
import play.api.mvc.{Action, Controller}
import services.eventbrite.GuardianLiveEventCache
import services.{SubscriptionService, TouchpointBackend}
import scala.util.Success

trait Test {
  def ok: Boolean
  def messages: Seq[String] = Nil
}

class BoolTest(name: String, exec: () => Boolean) extends Test {
  override def messages = List(s"Test $name failed, health check will fail")
  override def ok = exec()
}

class TierPricingTest(subscriptionService: SubscriptionService) extends Test {
  def getTierPricing: Option[Either[Map[Tier, List[String]], Map[Tier, Pricing]]] =
    subscriptionService.productCatalogSupplier.get().value.collect {
      case Success(catalog) => TierPricing(catalog).byTier
    }

  override def ok = getTierPricing.exists(_.isRight)
  override def messages = getTierPricing.fold[Seq[String]](Nil)(eReport =>
    eReport.left.toSeq.flatMap { report => report.collect { case (tier, errors) =>
      s"Incomplete pricing data for tier $tier. Errors: ${errors.mkString(", ")}; health check will fail"
    }
  })
}

object Healthcheck extends Controller {
  val zuoraSoapClient = TouchpointBackend.Normal.zuoraSoapClient
  val subscriptionService = TouchpointBackend.Normal.subscriptionService

  def tests = Seq(
    new BoolTest("Events", () => GuardianLiveEventCache.events.nonEmpty),
    new BoolTest("CloudWatch", () => CloudWatchHealth.hasPushedMetricSuccessfully),
    new BoolTest("ZuoraPing", () => zuoraSoapClient.lastPingTimeWithin(2.minutes)),
    new BoolTest("ZuoraProductRatePlans", () => subscriptionService.productRatePlanIdSupplier.get().value.exists(_.isSuccess)),
    new TierPricingTest(subscriptionService)
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
