import filters.CheckCacheHeadersFilter
import play.api.mvc.WithFilters
import services.{MemberService, EventbriteService}
import play.api.{ Play, Logger, Application, GlobalSettings }

object Global extends WithFilters(CheckCacheHeadersFilter) {
  override def onStart(app: Application) {
    EventbriteService.start()
    MemberService.start()
  }
}
