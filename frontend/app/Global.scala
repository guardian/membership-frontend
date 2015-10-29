import filters._
import monitoring.SentryLogging
import play.api.Application
import play.api.libs.concurrent.Akka
import play.api.mvc.WithFilters
import play.filters.csrf._
import play.api.Play.current
import play.api.libs.concurrent.Execution.Implicits._
import scala.concurrent.duration._
import services._

object Global extends WithFilters(RedirectMembersFilter, CheckCacheHeadersFilter, CSRFFilter(), Gzipper, AddEC2InstanceHeader) {
  override def onStart(app: Application) {
    SentryLogging.init()
    GuardianLiveEventService.start()
    LocalEventService.start()
    MasterclassEventService.start()
    GuardianContentService.start()

    TouchpointBackend.All.foreach { touchPoint =>
      Akka.system.scheduler.schedule(0.millis, 5.minutes) {
        touchPoint.subscriptionService.membershipCatalog.refresh()
      }
    }
  }
}
