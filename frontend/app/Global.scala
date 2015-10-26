import filters._
import monitoring.SentryLogging
import play.api.Application
import play.api.mvc.WithFilters
import play.filters.csrf._
import services._
import services.eventbrite.{GuardianLiveEventService, MasterclassEventService, LocalEventService}

object Global extends WithFilters(RedirectMembersFilter, CheckCacheHeadersFilter, CSRFFilter(), Gzipper, AddEC2InstanceHeader) {
  override def onStart(app: Application) {
    SentryLogging.init()

    GuardianLiveEventService.start()
    LocalEventService.start()
    MasterclassEventService.start()

    GuardianContentService.start()
  }
}
