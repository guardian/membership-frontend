import filters.CheckCacheHeadersFilter
import play.api.mvc.WithFilters

object Global extends WithFilters(CheckCacheHeadersFilter)