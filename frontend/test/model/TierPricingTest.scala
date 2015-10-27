package model

import com.gu.membership.salesforce.Tier._
import com.gu.membership.zuora.rest.{USD, GBP, Currency, ProductCatalog}
import com.gu.membership.zuora.rest.Readers._
import org.specs2.mutable.Specification
import services.SubscriptionService.membershipProductType
import utils.Resource

class TierPricingTest extends Specification {
  def internationalPricing(tuples: (Currency, Int, Int)*)=
    InternationalPricing(tuples.map {
      case (c, y, m) => c -> Pricing(c, y, m)
    }.toMap)

  def fromResource(fileName: String): TierPricing = {
    val json = Resource.getJson(fileName)
    val catalog = parseResponse[ProductCatalog](json).get.productsOfType(membershipProductType)
    TierPricing.fromProductCatalog(catalog)
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
        Supporter -> internationalPricing((GBP, 50, 5)),
        Partner -> internationalPricing((GBP, 135, 15)),
        Patron -> internationalPricing((GBP, 540, 60))
      ))
    }

    "parses a Zuora catalog with prices in multiple currencies" in {
      fromResource("model/zuora/json/product-catalog-multicurrency.json").byTier mustEqual Right(Map(
        Supporter -> internationalPricing((GBP, 50, 5), (USD, 80, 7)),
        Partner -> internationalPricing((GBP, 135, 15)),
        Patron -> internationalPricing((GBP, 540, 60))
      ))
    }

    "parses a Zuora product catalog with no pricing into a map of errors" in {
      fromResource("model/zuora/json/product-catalog-incomplete.json").byTier mustEqual Left(
        Map(
          Partner -> List("Cannot find GBP price"),
          Patron -> List("Cannot find a RatePlan (billingPeriod: Annual)", "Cannot find a RatePlan (billingPeriod: Month)")
        )
      )
    }
  }
}
