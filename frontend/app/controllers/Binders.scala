package controllers

import com.gu.i18n.CountryGroup
import com.gu.membership.salesforce.{PaidTier, Tier, FreeTier}
import play.api.mvc.PathBindable.{Parsing => PathParsing}
import play.api.mvc.QueryStringBindable.{Parsing => QueryParsing}


object Binders {
  implicit object bindableTier extends PathParsing[Tier](
    Tier.slugMap, _.slug, (key: String, e: Exception) => s"Cannot parse parameter $key as a Tier: ${e.getMessage}"
  )

  implicit object bindablePaidTier extends PathParsing[PaidTier](
    PaidTier.slugMap, _.slug, (key: String, e: Exception) => s"Cannot parse parameter $key as a PaidTier: ${e.getMessage}"
  )

  implicit object bindableFreeTier extends PathParsing[FreeTier](
    FreeTier.slugMap, _.slug, (key: String, e: Exception) => s"Cannot parse parameter $key as a FreeTier: ${e.getMessage}"
  )

  implicit object bindableCountryGroup extends QueryParsing[CountryGroup](
    id => CountryGroup.byId(id).get, _.id, (key: String, _: Exception) => s"Cannot parse parameter $key as a CountryGroup"
  )
}
