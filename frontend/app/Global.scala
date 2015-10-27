import filters._
import monitoring.SentryLogging
import play.api.Application
import play.api.mvc.WithFilters
import play.filters.csrf._
import services._
import services.eventbrite.{GuardianLiveEventCache, MasterclassEventCache, LocalEventCache}

object Global extends WithFilters(RedirectMembersFilter, CheckCacheHeadersFilter, CSRFFilter(), Gzipper, AddEC2InstanceHeader) {
  override def onStart(app: Application) {
    SentryLogging.init()

    GuardianLiveEventCache.start()
    LocalEventCache.start()
    MasterclassEventCache.start()

    GuardianContentService.start()
  }
}
