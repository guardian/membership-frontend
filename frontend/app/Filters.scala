
import javax.inject.Inject

import akka.stream.Materializer
import filters.{AddEC2InstanceHeader, CheckCacheHeadersFilter, Gzipper, RedirectMembersFilter}
import play.api.http.DefaultHttpFilters
import play.filters.csrf.CSRFFilter

class Filters @Inject()(
  redirectMembersFilter: RedirectMembersFilter,
  checkCacheHeadersFilter: CheckCacheHeadersFilter,
  csrfFilter: CSRFFilter,
  gzipper: Gzipper,
  addEC2InstanceHeader: AddEC2InstanceHeader)
  (implicit val mat: Materializer) extends DefaultHttpFilters(
  redirectMembersFilter,
  checkCacheHeadersFilter,
  csrfFilter,
  gzipper,
  addEC2InstanceHeader
)
