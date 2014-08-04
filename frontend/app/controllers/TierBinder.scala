package controllers

import com.gu.membership.salesforce.Tier
import play.api.mvc.PathBindable.Parsing

object TierBinder {

  implicit object bindableTier extends Parsing[Tier.Value](
    Tier.routeMap, _.toString.toLowerCase,
    (key: String, e: Exception) => s"Cannot parse parameter ${key} as a Tier: ${e.getMessage}"
  )
}
