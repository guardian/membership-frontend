package controllers

import com.gu.membership.salesforce.Tier
import play.api.mvc.PathBindable.Parsing

object TierBinder {

  implicit object bindableTier extends Parsing[Tier](
    Tier.slugMap, _.slug, (key: String, e: Exception) => s"Cannot parse parameter $key as a Tier: ${e.getMessage}"
  )
}
