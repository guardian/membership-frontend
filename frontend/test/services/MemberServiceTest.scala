package services

import com.gu.config.{DiscountRatePlan, DiscountRatePlanIds}
import com.gu.membership.FreeMembershipPlan
import com.gu.memsub.Status
import com.gu.memsub.Subscription.Feature.{Code, Id}
import com.gu.memsub.Subscription.{ProductRatePlanChargeId, ProductRatePlanId}
import com.gu.salesforce.Tier
import com.gu.salesforce.Tier.{friend, partner, patron, supporter}
import com.gu.zuora.rest
import com.gu.zuora.soap.models.Queries._
import model.{Books, FreeEventTickets}
import org.specs2.mutable.Specification

class MemberServiceTest extends Specification {

  "featureIdsForTier" should {
    val feature1 = Feature(id = Id("1"), code = Code("Books"))
    val feature2 = Feature(id = Id("2"), code = Code("Events"))
    val feature3 = Feature(id = Id("3"), code = Code("OtherFeature"))
    val features = Seq(feature1, feature2, feature3)

    val featureIds = MemberService.featureIdsForTier(features) _

    "return both books and events for patrons" in {
      featureIds(patron, Set(Books, FreeEventTickets)) mustEqual List(feature1, feature2).map(_.id)
      featureIds(patron, Set()) mustEqual List(feature1, feature2).map(_.id)
    }

    "return only one book or event for partner" in {
      featureIds(partner, Set(Books)) mustEqual List(feature1.id)
      featureIds(partner, Set(FreeEventTickets)) mustEqual List(feature2.id)
      featureIds(partner, Set(Books, FreeEventTickets)).size mustEqual 1
    }

    "return no features for supporters or friends" in {
      featureIds(supporter, Set(Books)) mustEqual List.empty
      featureIds(friend, Set(FreeEventTickets)) mustEqual List.empty
    }
  }

  "getRatePlansToRemove" should {

    val partnerPrpId = ProductRatePlanId("partner")
    val discountPrpId = ProductRatePlanId("discount")
    val manualPrpId = ProductRatePlanId("A manual discount")
    val discountPrpChargeId = ProductRatePlanChargeId("discount")

    val discounts = DiscountRatePlanIds(DiscountRatePlan(discountPrpId, discountPrpChargeId))

    val current = Seq(
      rest.RatePlan("id1", "prod", partnerPrpId.get, "", List.empty, List.empty),
      rest.RatePlan("id2", "discount", discountPrpId.get, "", List.empty, List.empty),
      rest.RatePlan("id3", "discount", manualPrpId.get, "", List.empty, List.empty)
    )

    "Remove any rate plans in the product catalog with discounts, leaving off any others" in {
      MemberService.getRatePlanIdsToRemove(current, _ == partnerPrpId, discounts) mustEqual Seq("id1", "id2")
    }
  }
}
