package controllers

import com.gu.membership.salesforce.{PaidTier, Tier, FreeTier}
import play.api.mvc.PathBindable.Parsing

object TierBinder {
  implicit object bindableTier extends Parsing[Tier](
    Tier.slugMap, _.slug, (key: String, e: Exception) => s"Cannot parse parameter $key as a Tier: ${e.getMessage}"
  )

  implicit object bindablePaidTier extends Parsing[PaidTier](
    PaidTier.slugMap, _.slug, (key: String, e: Exception) => s"Cannot parse parameter $key as a PaidTier: ${e.getMessage}"
  )

  implicit object bindableFreeTier extends Parsing[FreeTier](
    FreeTier.slugMap, _.slug, (key: String, e: Exception) => s"Cannot parse parameter $key as a FreeTier: ${e.getMessage}"
  )
}
