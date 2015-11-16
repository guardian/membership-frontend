package model

import com.gu.membership.salesforce.Tier._
import com.gu.membership.zuora.rest.ProductCatalog
import com.gu.membership.zuora.rest.Readers._
import org.specs2.mutable.Specification
import services.SubscriptionService.membershipProductType
import utils.Resource

class TierPricingTest extends Specification {
  def fromResource(fileName: String): TierPricing = {
    val json = Resource.getJson(fileName)
    val catalog = parseResponse[ProductCatalog](json).get.productsOfType(membershipProductType)
    TierPricing(catalog)
  }

  "TicketPricing" should {
    "excludes expired rate-plans" in {
      val tierPricing = fromResource("model/zuora/json/product-catalog-expired-partner.json")
      tierPricing.byTier mustEqual Left(Map(
        Partner -> List("Cannot find a RatePlan (billingPeriod: Month)")
      ))
    }

    "parses a Zuora product catalog into a tier-pricing map" in {
      fromResource("model/zuora/json/product-catalog.json").byTier mustEqual Right(Map(
        Supporter -> Pricing(50, 5),
        Partner -> Pricing(135, 15),
        Patron -> Pricing(540, 60)
      ))
    }

    "parses a Zuora product catalog with no pricing into a map of errors" in {
      fromResource("model/zuora/json/product-catalog-incomplete.json").byTier mustEqual Left(
        Map(
          Partner -> List("Cannot find a GBP price"),
          Patron -> List("Cannot find a RatePlan (billingPeriod: Annual)", "Cannot find a RatePlan (billingPeriod: Month)")
        )
      )
    }
  }
}
