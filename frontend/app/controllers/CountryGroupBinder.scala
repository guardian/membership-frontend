package controllers

import com.gu.i18n.CountryGroup
import play.api.mvc.QueryStringBindable.Parsing

object CountryGroupBinder {
  implicit object bindableCountryGroup extends Parsing[CountryGroup](
    id => CountryGroup.byId(id).get, _.id, (key: String, _: Exception) => s"Cannot parse parameter $key as a CountryGroup"
  )
}
