package com.gu.zuora.soap

import com.gu.zuora.soap.models.Queries.{ProductRatePlan, RatePlan}
import org.joda.time.LocalDate
import org.specs2.mutable.Specification

class ClientTest extends Specification {
  "childFilter" should {
    "return a filter used to query children of an object" in {
      val productRatePlan = ProductRatePlan("prpId", "prpName", "productId", LocalDate.now(), LocalDate.now())
      Client.childFilter[RatePlan, ProductRatePlan](productRatePlan) must_=== SimpleFilter("ProductRatePlanId", "prpId")
    }
  }

  "parentFilter" should {
    "return a filter used to query the parent of an object" in {
      val ratePlan = RatePlan("rpId", "rpName", "prpId")
      Client.parentFilter[RatePlan, ProductRatePlan](ratePlan, _.productRatePlanId) must_=== SimpleFilter("Id", "prpId")
    }
  }
}
