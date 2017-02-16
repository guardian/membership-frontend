package tracking

import com.gu.salesforce.Tier
import com.gu.salesforce.Tier.Partner
import com.gu.salesforce.Tier.Patron
import com.gu.salesforce.Tier.Supporter
import com.gu.salesforce.Tier.Friend

object AppnexusPixel {

  val thankYouPageIds:Map[Tier, Int] = Map(Partner() -> 793015, Patron() -> 793021, Supporter() ->793016, Friend() -> 793017)

  val landingPageIds:Map[Tier, Int] = Map(Supporter() -> 7269289)

  val checkoutPageIds:Map[Tier, Int] = Map(Partner() -> 7269292, Patron() -> 7269296, Supporter() -> 7269291)
}
