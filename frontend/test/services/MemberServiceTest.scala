package services

import com.gu.membership.model._
import com.gu.salesforce.PaidTier
import com.gu.salesforce.Tier.{Partner, Patron, Supporter}
import com.gu.zuora.soap.models.Queries._
import model.{Books, FreeEventTickets}
import org.specs2.mutable.Specification

class MemberServiceTest extends Specification {

  "featureIdsForTier" should {
    val feature1 = Feature(id = "1", code = "Books")
    val feature2 = Feature(id = "2", code = "Events")
    val feature3 = Feature(id = "3", code = "OtherFeature")
    val features = Seq(feature1, feature2, feature3)

    def plan(t: PaidTier) = PaidTierPlan.yearly(t, Current)

    val featureIds = MemberService.featureIdsForTier(features) _

    "return both books and events for patrons" in {
      featureIds(plan(Patron), Set(Books, FreeEventTickets)) mustEqual List(feature1, feature2).map(_.id)
      featureIds(plan(Patron), Set()) mustEqual List(feature1, feature2).map(_.id)
    }

    "return only one book or event for partner" in {
      featureIds(plan(Partner), Set(Books)) mustEqual List(feature1.id)
      featureIds(plan(Partner), Set(FreeEventTickets)) mustEqual List(feature2.id)
      featureIds(plan(Partner), Set(Books, FreeEventTickets)).size mustEqual 1
    }

    "return no features for supporters or friends" in {
      featureIds(plan(Supporter), Set(Books)) mustEqual List.empty
      featureIds(FriendTierPlan.current, Set(FreeEventTickets)) mustEqual List.empty
    }
  }
}
