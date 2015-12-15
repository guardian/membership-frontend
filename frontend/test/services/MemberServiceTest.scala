package services

import com.gu.membership.model._
import com.gu.membership.salesforce.PaidTier
import com.gu.membership.salesforce.Tier.{Partner, Patron, Supporter}
import com.gu.membership.util.FutureSupplier
import com.gu.membership.zuora.soap.ClientWithFeatureSupplier
import com.gu.membership.zuora.soap.models.Queries._
import model.{Books, FreeEventTickets}
import org.specs2.mutable.Specification
import play.api.test.PlaySpecification
import org.specs2.mock.Mockito

import scala.concurrent.Future

class MemberServiceTest extends Specification with PlaySpecification with Mockito {

  "featureIdsForTier" should {
    val feature1 = Feature(id = "1", code = "Books")
    val feature2 = Feature(id = "2", code = "Events")
    val feature3 = Feature(id = "3", code = "OtherFeature")
    val features = Future.successful(Seq(feature1, feature2, feature3))

    val client = mock[ClientWithFeatureSupplier]
    client.featuresSupplier returns new FutureSupplier[Seq[Feature]](features)

    val zuoraService = new ZuoraService(???, client, ???, ???)

    val service = new MemberService(???, ???, zuoraService = zuoraService, ???, ???, ???)

    def plan(t: PaidTier) = PaidTierPlan.yearly(t, Current)

    "return both books and events for patrons" in {
      await(service.featureIdsForTier(plan(Patron), Set(Books, FreeEventTickets))) mustEqual List(feature1, feature2).map(_.id)
      await(service.featureIdsForTier(plan(Patron), Set())) mustEqual List(feature1, feature2).map(_.id)
    }

    "return only one book or event for partner" in {
      await(service.featureIdsForTier(plan(Partner), Set(Books))) mustEqual List(feature1.id)
      await(service.featureIdsForTier(plan(Partner), Set(FreeEventTickets))) mustEqual List(feature2.id)
      await(service.featureIdsForTier(plan(Partner), Set(Books, FreeEventTickets))).size mustEqual 1
    }

    "return no features for supporters or friends" in {
      await(service.featureIdsForTier(plan(Supporter), Set(Books)))mustEqual List.empty
      await(service.featureIdsForTier(FriendTierPlan.current, Set(FreeEventTickets))) mustEqual List.empty
    }
  }
}
