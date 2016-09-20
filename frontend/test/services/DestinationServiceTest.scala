package services
import com.github.nscala_time.time.Imports._
import com.gu.contentapi.client.parser.JsonParser
import com.gu.i18n.{Currency, GBP}
import com.gu.membership.PaidMembershipPlan
import com.gu.memsub.Subscription.{PaidMembershipSub, ProductRatePlanId}
import com.gu.memsub._
import com.gu.salesforce.Tier.Partner
import com.gu.salesforce._
import model.Eventbrite.EBAccessCode
import model.EventbriteTestObjects._
import model.{ContentDestination, EventDestination}
import org.joda.time.LocalDate
import org.specs2.mutable.Specification
import play.api.mvc.Session
import play.api.test.FakeRequest
import utils.Resource

import scalaz.Id._

class DestinationServiceTest extends Specification {

  "DestinationService" should {

    val destinationService = new DestinationService[Id](
      getBookableEvent = _ => Some(TestRichEvent(eventWithName().copy(id = "0123456"))),
      capiItemQuery = _ => JsonParser.parseItemThrift(Resource.get("model/content.api/item.json")),
      createCode = (_, _) => Some(EBAccessCode("some-discount-code", 2))
    )

    def createRequestWithSession(newSessions: (String, String)*) = {

      val testMember = Contact("id", None, Some("fn"), "ln", "email", new DateTime(), "contactId", "accountId")
      val testSub: PaidMembershipSub = new Subscription(
        id = Subscription.Id(""),
        name = Subscription.Name(""),
        accountId = Subscription.AccountId(""),
        currency = Currency.all.head,
        productRatePlanId = Subscription.ProductRatePlanId(""),
        productName = "productName",
        startDate = new LocalDate("2015-01-01"),
        termStartDate = new LocalDate("2015-01-01"),
        termEndDate = new LocalDate("2016-01-01"),
        features = Nil,
        promoCode = None,
        casActivationDate = None,
        isCancelled = false,
        ratePlanId = "",
        isPaid = true
      ) with PaidPS[PaidMembershipPlan[Status, PaidTier, BillingPeriod]] {
        override def recurringPrice: Price = new Price(0.1f, GBP)
        override def firstPaymentDate: LocalDate = new LocalDate("2015-01-01")
        override def chargedThroughDate: Option[LocalDate] = None
        override def priceAfterTrial: Price = recurringPrice
        override def hasPendingAmendment: Boolean = false
        override def plan = new PaidMembershipPlan[Current, Partner, Month](Current(), Tier.Partner(), Month(), ProductRatePlanId(""), PricingSummary(Map(GBP -> Price(0.1f, GBP))))
      }

      val testSubscriber: Subscriber.Member = Subscriber(testSub, testMember)
      (Session(newSessions.toMap), testSubscriber)
    }

    "should return a content destination url if join-referrer is in the request session" in {
      val (request, _) = createRequestWithSession("join-referrer" -> "http://www.theguardian.com/membership/2015/apr/17/guardian-live-diversity-in-the-arts")
      val result = destinationService.contentDestinationFor(request)
      result.get.item.content.id mustEqual "membership/2015/apr/17/guardian-live-diversity-in-the-arts"
    }

    "should return an event destination url if preJoinReturnUrl is in the request session" in {
      val (request, member) = createRequestWithSession("preJoinReturnUrl" -> "/event/0123456/buy")
      val result = destinationService.eventDestinationFor(request, member)
      result.get.event.id mustEqual "0123456"
    }

    "should return either content or event destination if both are supplied" in {
      val (request, member) = createRequestWithSession("join-referrer" -> "http://www.theguardian.com/membership/2015/apr/17/guardian-live-diversity-in-the-arts", "preJoinReturnUrl" -> "/event/0123456/buy")

      destinationService.returnDestinationFor(request, member) match {
        case Some(EventDestination(event, _)) => event.id mustEqual "0123456"
        case Some(ContentDestination(item)) => item.content.id mustEqual "membership/2015/apr/17/guardian-live-diversity-in-the-arts"
        case _ => 1 mustEqual 0 // surely this can be improved!
      }
    }
  }
}
