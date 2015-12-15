package model

import com.gu.salesforce.Tier

import scala.language.implicitConversions

object TierOrdering {
  implicit def upgradeOrdering(t: Tier): Ordered[Tier] = new Ordered[Tier] {
    override def compare(that: Tier): Int = Tier.all.indexOf(t) compare Tier.all.indexOf(that)
  }
}
