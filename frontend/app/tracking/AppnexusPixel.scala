package tracking

import com.gu.salesforce.Tier
import com.gu.salesforce.Tier.Partner
import com.gu.salesforce.Tier.Patron
import com.gu.salesforce.Tier.Supporter
import com.gu.salesforce.Tier.Friend

object AppnexusPixel {

  def getThankYouPageId(tier: Tier) : Option[Int] = {
    tier match {
      case Partner() => Some(793015)
      case Patron() => Some(793021)
      case Supporter() => Some(793016)
      case Friend() => Some(793017)
      case _ => None
    }
  }

  def getLandingPageID(tier: Tier) : Option[Int] = {
    tier match {
      case Supporter() => Some(7269289)
      case _ => None
    }
  }

  def getCheckoutPageID(tier: Tier) : Option[Int] = {
    tier match {
      case Partner() => Some(7269292)
      case Patron() => Some(7269296)
      case Supporter() => Some(7269291)
      case _ => None
    }
  }
}
